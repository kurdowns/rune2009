package core.game.world

import core.ServerConstants
import org.json.simple.JSONObject
import java.io.FileInputStream
import java.io.IOException
import java.util.*
/**
 * Represents the game settings used for this game instance.
 * @author Vexia
 */
class GameSettings
/**
 * Constructs a new `GameSettings` `Object`.
 * @param name the name.
 * @param beta the beta.
 * @param type the game type.
 * @param gui if gui is enabled.
 * @param worldId the world id.
 * @param countryIndex The country index.
 * @param members If the world is members only.
 * @param msAddress The address of the Management server.
 */ internal constructor(
        /**
         * The name of the namme.
         */
        val name: String,
        /**
         * If the game is in beta mode.
         */
        val isBeta: Boolean,
        /**
         * If the game is in developer mode.
         */
        val isDevMode: Boolean,
        /**
         * If the gui is enabled.
         */
        val isGui: Boolean,
        /**
         * The world id of the server.
         */
        val worldId: Int,
        /**
         * The country index.
         */
        val countryIndex: Int,
        /**
         * The activity.
         */
        val activity: String,
        /**
         * If the world is members only.
         */
        val isMembers: Boolean,
        /**
         * If the world is a pvp world.
         */
        val isPvp: Boolean,
        /**
         * If only quick chat can be used on the world.
         */
        val isQuickChat: Boolean,
        /**
         * If lootshare option is enabled on this world.
         */
        val isLootshare: Boolean,
        /**
         * The address of the Management server.
         */
        val msAddress: String,
        val default_xp_rate: Double,
        val allow_slayer_reroll: Boolean,
        val enable_default_clan: Boolean,
        val enable_bots: Boolean,
        val autostock_ge: Boolean,
        val allow_token_purchase: Boolean,

        /**"Lobby" interface
         * The message of the week models to display
         * 15 & 22 = keys & lock || 16 = fly swat || 17 = person with question marks || 18 & 447 = wise old man
         * 19 = man & woman with mouth closed || 20 = man & lock & key || 21 = closed chests
         * 23 = snowmen || 405 = Construction houses || 622 = Two sets of 3 people range, mage, melee
         * 623 = Woodcutting || 679 = Summoning || 715 = Easter || 800 = Halloween
         * Any value that isn't one listed above = random selection
         */
        val message_model: Int,

        /**"Lobby" interface
         * The message of the week text
         * The "child" for writing text to these interfaces is located inside of LoginConfiguration.java
         * method: getMessageChild
         */
        val message_string: String
        ) {
    val isHosted: Boolean
        get() = !isDevMode

    override fun toString(): String {
        return "GameSettings [name=$name, debug=$isBeta, devMode=$isDevMode, gui=$isGui, worldId=$worldId]"
    }

    companion object {
        /**
         * Parses a JSONObject and creates a new GameSettings object from it.
         * @param data the JSONObject to parse.
         * @return the settings object.
         * @author Ceikry
         */
        fun parse(data: JSONObject): GameSettings? {
            val name = ServerConstants.SERVER_NAME
            val debug = data["debug"] as Boolean
            val dev = data["dev"] as Boolean
            val startGui = data["startGui"] as Boolean
            val worldId = data["worldID"].toString().toInt()
            val countryId = data["countryID"].toString().toInt()
            val activity = data["activity"].toString()
            val pvpWorld = data["pvpWorld"] as Boolean
            val msip = data["msip"].toString()
            val default_xp_rate = data["default_xp_rate"].toString().toDouble()
            val allow_slayer_reroll = data["allow_slayer_reroll"] as Boolean
            val enable_default_clan = data["enable_default_clan"] as Boolean
            val enable_bots = data["enable_bots"] as Boolean
            val autostock_ge = data["autostock_ge"] as Boolean
            val allow_token_purchase = data["allow_token_purchase"] as Boolean
            val message_of_the_week_identifier = data["message_of_the_week_identifier"].toString().toInt()
            val message_of_the_week_text = data["message_of_the_week_text"].toString()
            return GameSettings(name,debug,dev,startGui,worldId,countryId,activity,true,pvpWorld,false,false,msip,default_xp_rate,allow_slayer_reroll,enable_default_clan,enable_bots,autostock_ge,allow_token_purchase,message_of_the_week_identifier,message_of_the_week_text)
        }

        /**
         * Gets the properties.
         * @param path the path.
         * @return the properties.
         */
        private fun getProperties(path: String): Properties {
            val file: FileInputStream
            val properties = Properties()
            try {
                file = FileInputStream(path)
                properties.load(file)
            } catch (e: IOException) {
                println("Warning: Could not find file in " + System.getProperty("user.dir") + path)
                e.printStackTrace()
            }
            return properties
        }
    }
}