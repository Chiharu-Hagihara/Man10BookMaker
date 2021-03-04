package red.man10

import com.github.syari.spigot.api.config.config
import com.github.syari.spigot.api.config.type.ConfigDataType
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class BookMakerConfigManager {

    companion object {
        var pl: BookMakerPlugin? = null
    }

    fun returnConfigManager(plugin: BookMakerPlugin) : BookMakerConfigManager{
        pl = plugin
        return BookMakerConfigManager()
    }

    fun loadSpawn(sender: CommandSender) {

        pl?.config(sender, "spawn.yml") {

            val world = get("spawn.world", ConfigDataType.String)!!

            val x = get("spawn.x", ConfigDataType.Double)!!

            val y = get("spawn.x", ConfigDataType.Double)!!

            val z = get("spawn.x", ConfigDataType.Double)!!

            pl?.spawn = Location(Bukkit.getWorld(world), x, y, z)

        }

    }

    fun loadConfig(sender: CommandSender){

        pl?.config(sender, "config.yml") {

            if (config.getKeys(true).isNullOrEmpty()) {
                sender.sendMessage("${pl?.prefix}${ChatColor.RED}config.ymlにゲームがありません！")
                return@config
            }

            pl!!.gameManager.setUpGames(config, (sender as Player))
            sender.sendMessage(pl!!.prefix + "config.ymlのゲームが読み込まれました。")

        }
    }

}