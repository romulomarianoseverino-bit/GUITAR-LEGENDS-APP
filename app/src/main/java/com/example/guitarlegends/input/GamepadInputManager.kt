package com.example.guitarlegends.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.guitarlegends.model.ControllerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ControllerKind(val label: String) {
    NONE("Sem controle"),
    PLAYSTATION_DS4("DualShock 4 / PlayStation"),
    XBOX_STYLE("Controle Xbox"),
    SWITCH_STYLE("Controle Switch"),
    GENERIC_GAMEPAD("Controle Bluetooth / USB")
}

data class ConnectedControllerInfo(
    val isConnected: Boolean = false,
    val deviceId: Int = -1,
    val deviceName: String = "",
    val kind: ControllerKind = ControllerKind.NONE,
    val statusMessage: String = "Nenhum controle Bluetooth conectado (Touchscreen ativo)"
)

/**
 * Gerenciador de controles Bluetooth e USB (DualShock 4 PS4, Xbox, Switch e Gamepads Android).
 * - Detecta conexão, desconexão e reconexão via InputDeviceListener.
 * - Suporta acordes simultâneos com múltiplas pistas pressionadas ao mesmo tempo.
 * - Desambigua gatilhos analógicos (AXIS_LTRIGGER/BRAKE e AXIS_RTRIGGER/GAS) com de-duplicação.
 */
class GamepadInputManager(context: Context) {

    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager

    private val _controllerInfo = MutableStateFlow(ConnectedControllerInfo())
    val controllerInfo: StateFlow<ConnectedControllerInfo> = _controllerInfo.asStateFlow()

    private val _lastCapturedKeyCode = MutableStateFlow<Int?>(null)
    val lastCapturedKeyCode: StateFlow<Int?> = _lastCapturedKeyCode.asStateFlow()

    private var isCapturingForRemap: Boolean = false
    private val activeHardwareLanes = mutableSetOf<Int>()
    private var leftAnalogTriggerPressed = false
    private var rightAnalogTriggerPressed = false

    private val deviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            refreshConnectedDevices(reconnectedHint = true)
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            activeHardwareLanes.clear()
            leftAnalogTriggerPressed = false
            rightAnalogTriggerPressed = false
            refreshConnectedDevices(reconnectedHint = false, removedId = deviceId)
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            refreshConnectedDevices(reconnectedHint = false)
        }
    }

    init {
        inputManager?.registerInputDeviceListener(deviceListener, Handler(Looper.getMainLooper()))
        refreshConnectedDevices(reconnectedHint = false)
    }

    fun startKeyCapture() {
        _lastCapturedKeyCode.value = null
        isCapturingForRemap = true
    }

    fun stopKeyCapture() {
        isCapturingForRemap = false
    }

    fun refreshConnectedDevices(reconnectedHint: Boolean = false, removedId: Int? = null) {
        val deviceIds = inputManager?.inputDeviceIds ?: InputDevice.getDeviceIds()
        var foundDevice: InputDevice? = null
        for (id in deviceIds) {
            if (id == removedId) continue
            val dev = InputDevice.getDevice(id) ?: continue
            if (isGameControllerDevice(dev)) {
                foundDevice = dev
                break
            }
        }

        if (foundDevice != null) {
            val kind = classifyController(foundDevice)
            val prefix = if (reconnectedHint) "Reconectado: " else "Conectado: "
            _controllerInfo.value = ConnectedControllerInfo(
                isConnected = true,
                deviceId = foundDevice.id,
                deviceName = foundDevice.name ?: kind.label,
                kind = kind,
                statusMessage = "$prefix${foundDevice.name ?: kind.label}"
            )
        } else {
            _controllerInfo.value = ConnectedControllerInfo(
                isConnected = false,
                deviceId = -1,
                deviceName = "",
                kind = ControllerKind.NONE,
                statusMessage = if (removedId != null) {
                    "Controle desconectado — usando Touchscreen automaticamente"
                } else {
                    "Nenhum controle Bluetooth conectado (Touchscreen ativo)"
                }
            )
        }
    }

    fun handleKeyEvent(
        event: KeyEvent,
        settings: ControllerSettings,
        isGameplayScreen: Boolean,
        isPaused: Boolean,
        onLaneDown: (Int) -> Unit,
        onLaneUp: (Int) -> Unit,
        onStrum: () -> Unit,
        onStarPower: () -> Unit,
        onTogglePause: () -> Unit,
        onMenuBack: () -> Unit
    ): Boolean {
        val fromGamepad = isGamepadSource(event.source) || isKnownGamepadKey(event.keyCode)
        if (!fromGamepad) return false

        // Garante detecção imediata caso o dispositivo Bluetooth tenha acabado de acordar
        if (!_controllerInfo.value.isConnected && event.device != null) {
            refreshConnectedDevices(reconnectedHint = true)
        }

        if (isCapturingForRemap && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            _lastCapturedKeyCode.value = event.keyCode
            isCapturingForRemap = false
            return true
        }

        if (!settings.enabled) return false

        // Ignora repetições automáticas ao segurar botão no gamepad
        if (event.repeatCount > 0) return true

        val keyCode = event.keyCode
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val isUp = event.action == KeyEvent.ACTION_UP

        // Botão Pause / Options funciona tanto jogando quanto pausado
        if (keyCode == settings.pauseKeyCode || keyCode == KeyEvent.KEYCODE_BUTTON_START) {
            if (isDown && isGameplayScreen) {
                onTogglePause()
            }
            return true
        }

        if (!isGameplayScreen || isPaused) {
            // Navegação e retorno em menus / diálogo de pausa
            if (isDown && (keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_BACK)) {
                onMenuBack()
                return true
            }
            return false // Deixa foco do Compose processar DPAD / A (Cross) naturalmente
        }

        // Em gameplay ativo: checa pistas configuradas (suporte a acordes simultâneos)
        val laneIdx = settings.laneKeyCodes.indexOf(keyCode)
        if (laneIdx in 0..4) {
            if (isDown) {
                if (activeHardwareLanes.add(laneIdx)) {
                    onLaneDown(laneIdx)
                }
            } else if (isUp) {
                if (activeHardwareLanes.remove(laneIdx)) {
                    onLaneUp(laneIdx)
                }
            }
            return true
        }

        if (keyCode == settings.starPowerKeyCode && isDown) {
            onStarPower()
            return true
        }

        if (keyCode == settings.strumKeyCode && isDown) {
            onStrum()
            return true
        }

        return false
    }

    fun handleMotionEvent(
        event: MotionEvent,
        settings: ControllerSettings,
        isGameplayScreen: Boolean,
        isPaused: Boolean,
        onLaneDown: (Int) -> Unit,
        onLaneUp: (Int) -> Unit
    ): Boolean {
        if (!settings.enabled || !settings.useAnalogTriggers) return false
        if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK &&
            (event.source and InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD
        ) {
            return false
        }
        if (!isGameplayScreen || isPaused) return false

        val lVal = maxOf(
            event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
            event.getAxisValue(MotionEvent.AXIS_BRAKE)
        )
        val rVal = maxOf(
            event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            event.getAxisValue(MotionEvent.AXIS_GAS)
        )

        val l2Lane = settings.laneKeyCodes.indexOf(KeyEvent.KEYCODE_BUTTON_L2)
        if (l2Lane in 0..4) {
            val pressedNow = lVal >= 0.45f
            if (pressedNow != leftAnalogTriggerPressed) {
                leftAnalogTriggerPressed = pressedNow
                if (pressedNow) {
                    if (activeHardwareLanes.add(l2Lane)) onLaneDown(l2Lane)
                } else {
                    if (activeHardwareLanes.remove(l2Lane)) onLaneUp(l2Lane)
                }
            }
        }

        val r2Lane = settings.laneKeyCodes.indexOf(KeyEvent.KEYCODE_BUTTON_R2)
        if (r2Lane in 0..4) {
            val pressedNow = rVal >= 0.45f
            if (pressedNow != rightAnalogTriggerPressed) {
                rightAnalogTriggerPressed = pressedNow
                if (pressedNow) {
                    if (activeHardwareLanes.add(r2Lane)) onLaneDown(r2Lane)
                } else {
                    if (activeHardwareLanes.remove(r2Lane)) onLaneUp(r2Lane)
                }
            }
        }
        return true
    }

    fun releaseAllActiveLanes(onLaneUp: (Int) -> Unit) {
        val copy = activeHardwareLanes.toList()
        activeHardwareLanes.clear()
        leftAnalogTriggerPressed = false
        rightAnalogTriggerPressed = false
        copy.forEach(onLaneUp)
    }

    fun release() {
        runCatching { inputManager?.unregisterInputDeviceListener(deviceListener) }
    }

    companion object {
        fun isGameControllerDevice(device: InputDevice): Boolean {
            if (device.isVirtual) return false
            val sources = device.sources
            return ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) ||
                ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
        }

        fun classifyController(device: InputDevice): ControllerKind {
            val name = (device.name ?: "").lowercase()
            return when {
                name.contains("wireless controller") || name.contains("dualshock") ||
                    name.contains("dualsense") || name.contains("playstation") ||
                    device.vendorId == 0x054C -> ControllerKind.PLAYSTATION_DS4
                name.contains("xbox") || device.vendorId == 0x045E -> ControllerKind.XBOX_STYLE
                name.contains("nintendo") || name.contains("pro controller") ||
                    name.contains("joy-con") || device.vendorId == 0x057E -> ControllerKind.SWITCH_STYLE
                else -> ControllerKind.GENERIC_GAMEPAD
            }
        }

        private fun isGamepadSource(source: Int): Boolean {
            return (source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
                (source and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        }

        private fun isKnownGamepadKey(keyCode: Int): Boolean = keyCode in setOf(
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR
        )
    }
}
