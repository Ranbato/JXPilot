package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/packet.h
// ---------------------------------------------------------------------------

/**
 * Client/server UDP packet type codes.
 * Maps to the `PKT_*` `#define` constants in common/packet.h.
 */
object PacketType {
    const val KEYBOARD_SIZE: Int = 9 // bytes in a keyboard packet

    // 0 – 9
    const val UNDEFINED: Int = 0
    const val VERIFY: Int = 1
    const val REPLY: Int = 2
    const val PLAY: Int = 3
    const val QUIT: Int = 4
    const val MESSAGE: Int = 5
    const val START: Int = 6
    const val END: Int = 7
    const val SELF: Int = 8
    const val DAMAGED: Int = 9

    // 10 – 19
    const val CONNECTOR: Int = 10
    const val REFUEL: Int = 11
    const val SHIP: Int = 12
    const val ECM: Int = 13
    const val PAUSED: Int = 14
    const val ITEM: Int = 15
    const val MINE: Int = 16
    const val BALL: Int = 17
    const val MISSILE: Int = 18
    const val SHUTDOWN: Int = 19

    // 20 – 29
    const val STRING: Int = 20
    const val DESTRUCT: Int = 21
    const val RADAR: Int = 22
    const val TARGET: Int = 23
    const val KEYBOARD: Int = 24
    const val SEEK: Int = 25
    const val SELF_ITEMS: Int = 26
    const val TEAM_SCORE: Int = 27
    const val PLAYER: Int = 28
    const val SCORE: Int = 29

    // 30 – 39
    const val FUEL: Int = 30
    const val BASE: Int = 31
    const val CANNON: Int = 32
    const val LEAVE: Int = 33
    const val POWER: Int = 34
    const val POWER_S: Int = 35
    const val TURNSPEED: Int = 36
    const val TURNSPEED_S: Int = 37
    const val TURNRESISTANCE: Int = 38
    const val TURNRESISTANCE_S: Int = 39

    // 40 – 49
    const val WAR: Int = 40
    const val MAGIC: Int = 41
    const val RELIABLE: Int = 42
    const val ACK: Int = 43
    const val FASTRADAR: Int = 44
    const val TRANS: Int = 45
    const val ACK_CANNON: Int = 46
    const val ACK_FUEL: Int = 47
    const val ACK_TARGET: Int = 48
    const val SCORE_OBJECT: Int = 49

    // 50 – 59
    const val AUDIO: Int = 50
    const val TALK: Int = 51
    const val TALK_ACK: Int = 52
    const val TIME_LEFT: Int = 53
    const val LASER: Int = 54
    const val DISPLAY: Int = 55
    const val EYES: Int = 56
    const val SHAPE: Int = 57
    const val MOTD: Int = 58
    const val LOSEITEM: Int = 59

    // 60 – 69
    const val APPEARING: Int = 60
    const val TEAM: Int = 61
    const val POLYSTYLE: Int = 62
    const val ACK_POLYSTYLE: Int = 63
    const val MODIFIERS: Int = 68
    const val FASTSHOT: Int = 69

    // 70 – 79
    const val THRUSTTIME: Int = 70
    const val MODIFIERBANK: Int = 71
    const val SHIELDTIME: Int = 72
    const val POINTER_MOVE: Int = 73
    const val REQUEST_AUDIO: Int = 74
    const val ASYNC_FPS: Int = 75
    const val TIMING: Int = 76
    const val PHASINGTIME: Int = 77
    const val ROUNDDELAY: Int = 78
    const val WRECKAGE: Int = 79

    // 80 – 89
    const val ASTEROID: Int = 80
    const val WORMHOLE: Int = 81

    // Status replies
    const val FAILURE: Int = 101
    const val SUCCESS: Int = 102

    // Optimised packet base (128 + color + x + y)
    const val DEBRIS: Int = 128
}
