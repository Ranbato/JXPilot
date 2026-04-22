package org.lambertland.kxpilot.common

// ---------------------------------------------------------------------------
// Ported from: common/keys.h
// ---------------------------------------------------------------------------

/**
 * Keyboard action identifiers shared between client and server.
 * Maps to C `keys_t` enum in common/keys.h.
 *
 * Keys above [NUM_KEYS] are client-only (not transmitted in the key vector).
 */
enum class Key {
    // Shared keys (0 – NUM_KEYS-1)
    KEY_DUMMY, // 0
    KEY_LOCK_NEXT,
    KEY_LOCK_PREV,
    KEY_LOCK_CLOSE,
    KEY_CHANGE_HOME,
    KEY_SHIELD, // 5
    KEY_FIRE_SHOT,
    KEY_FIRE_MISSILE,
    KEY_FIRE_TORPEDO,
    KEY_TOGGLE_NUCLEAR,
    KEY_FIRE_HEAT, // 10
    KEY_DROP_MINE,
    KEY_DETACH_MINE,
    KEY_TURN_LEFT,
    KEY_TURN_RIGHT,
    KEY_SELF_DESTRUCT,
    KEY_LOSE_ITEM,
    KEY_PAUSE,
    KEY_TANK_DETACH,
    KEY_TANK_NEXT,
    KEY_TANK_PREV, // 20
    KEY_TOGGLE_VELOCITY,
    KEY_TOGGLE_CLUSTER,
    KEY_SWAP_SETTINGS,
    KEY_REFUEL,
    KEY_CONNECTOR,
    KEY_UNUSED_26,
    KEY_UNUSED_27,
    KEY_UNUSED_28,
    KEY_UNUSED_29,
    KEY_THRUST, // 30
    KEY_CLOAK,
    KEY_ECM,
    KEY_DROP_BALL,
    KEY_TRANSPORTER,
    KEY_TALK,
    KEY_FIRE_LASER,
    KEY_LOCK_NEXT_CLOSE,
    KEY_TOGGLE_COMPASS,
    KEY_TOGGLE_MINI,
    KEY_TOGGLE_SPREAD, // 40
    KEY_TOGGLE_POWER,
    KEY_TOGGLE_AUTOPILOT,
    KEY_TOGGLE_LASER,
    KEY_EMERGENCY_THRUST,
    KEY_TRACTOR_BEAM,
    KEY_PRESSOR_BEAM,
    KEY_CLEAR_MODIFIERS,
    KEY_LOAD_MODIFIERS_1,
    KEY_LOAD_MODIFIERS_2,
    KEY_LOAD_MODIFIERS_3, // 50
    KEY_LOAD_MODIFIERS_4,
    KEY_SELECT_ITEM,
    KEY_PHASING,
    KEY_REPAIR,
    KEY_TOGGLE_IMPLOSION,
    KEY_REPROGRAM,
    KEY_LOAD_LOCK_1,
    KEY_LOAD_LOCK_2,
    KEY_LOAD_LOCK_3,
    KEY_LOAD_LOCK_4, // 60
    KEY_EMERGENCY_SHIELD,
    KEY_HYPERJUMP,
    KEY_DETONATE_MINES,
    KEY_DEFLECTOR,
    KEY_UNUSED_65,
    KEY_UNUSED_66,
    KEY_UNUSED_67,
    KEY_UNUSED_68,
    KEY_UNUSED_69,
    KEY_UNUSED_70, // 70
    KEY_UNUSED_71,

    // Client-only keys (not placed in the server key-vector; ordinal >= NUM_KEYS)
    KEY_MSG_1,
    KEY_MSG_2,
    KEY_MSG_3,
    KEY_MSG_4,
    KEY_MSG_5,
    KEY_MSG_6,
    KEY_MSG_7,
    KEY_MSG_8,
    KEY_MSG_9,
    KEY_MSG_10,
    KEY_MSG_11,
    KEY_MSG_12,
    KEY_MSG_13,
    KEY_MSG_14,
    KEY_MSG_15,
    KEY_MSG_16,
    KEY_MSG_17,
    KEY_MSG_18,
    KEY_MSG_19,
    KEY_MSG_20,
    KEY_ID_MODE,
    KEY_TOGGLE_OWNED_ITEMS,
    KEY_TOGGLE_MESSAGES,
    KEY_POINTER_CONTROL,
    KEY_TOGGLE_RECORD,
    KEY_TOGGLE_SOUND,
    KEY_PRINT_MSGS_STDOUT,
    KEY_TALK_CURSOR_LEFT,
    KEY_TALK_CURSOR_RIGHT,
    KEY_TALK_CURSOR_UP,
    KEY_TALK_CURSOR_DOWN,
    KEY_SWAP_SCALEFACTOR,
    KEY_TOGGLE_RADAR_SCORE,
    KEY_INCREASE_POWER,
    KEY_DECREASE_POWER,
    KEY_INCREASE_TURNSPEED,
    KEY_DECREASE_TURNSPEED,
    KEY_TOGGLE_FULLSCREEN,
    KEY_EXIT,
    KEY_YES,
    KEY_NO,
    ;

    companion object {
        /** Number of keys shared with the server (size of the key bit-vector). */
        const val NUM_KEYS: Int = 72

        init {
            check(KEY_UNUSED_71.ordinal + 1 == NUM_KEYS) {
                "Key.NUM_KEYS ($NUM_KEYS) is out of sync with KEY_UNUSED_71.ordinal+1 (${KEY_UNUSED_71.ordinal + 1})"
            }
        }
    }
}
