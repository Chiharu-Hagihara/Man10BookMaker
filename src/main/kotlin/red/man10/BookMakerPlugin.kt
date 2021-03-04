package red.man10

import com.github.syari.spigot.api.command.command
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

enum class GameStatus(val rawValue :Int)  {
    OFF(1),
    JOIN(2),
    BET(3),
    FIGHT(4)
}

class BookMakerPlugin: JavaPlugin() {

    val gui = BookMakerGUI().returnGUI(this)
    val gameManager = BookMakerGameManager().returnGameManager(this)
    val listener = BookMakerListener().returnListener(this)
    val configManager = BookMakerConfigManager().returnConfigManager(this)
    var sidebar: BookMakerSidebar? = null
    var data: BookMakerData? = null // = BookMakerData().returnData(this)

    var isLocked = true

    var vault: VaultManager? = null

    val prefix = "§l[§a§lm§6§lBookMaker§f§l]§r "

    var worldguard: WorldGuardPlugin? = null

    var freezedPlayer = mutableListOf<UUID>()

    var spawn: Location? = null

    override fun onEnable() {
        logger.info("Man10BookMaker Enabled")
        server.pluginManager.registerEvents(listener, this)

        vault = VaultManager(this)

        worldguard = WorldGuardPlugin.inst()

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mb.")

        sidebar = BookMakerSidebar().returnSidebar(this)
        data = BookMakerData().returnData(this)
    }

    override fun onDisable() {
        sidebar!!.removeAll()
        for (gameID in gameManager.runningGames.keys) {
            gameManager.stopGame(gameID)
        }
        // Plugin shutdown logic
        logger.info("Man10BookMaker Disabled")
    }

    fun registerCommand() {

        this.command("mb") {

            execute {

                if (sender !is Player) {
                    sender.sendMessage("${prefix}${ChatColor.RED}コマンドはプレイヤー以外から実行できません。")
                    return@execute
                }

                val player = sender as Player

                if (args.isNullOrEmpty()) {

                    if (!sender.hasPermission("mp.play")) {
                        sender.sendMessage("${prefix}${ChatColor.RED}権限がありません。")
                        return@execute
                    }

                    if (isLocked) {
                        sender.sendMessage("${prefix}${ChatColor.RED}ブックメーカーは現在OFFになっています。")
                        return@execute
                    }

                    gui.openTopMenu(player)

                    return@execute
                }

                when (args[0]) {

                    "help" -> {
                        showHelp(player)
                        return@execute
                    }

                    "view" -> {
                        if (!player.hasPermission("mb.view")) {
                            player.sendMessage("${prefix}${ChatColor.RED}権限がありません。")
                            return@execute
                        }

                        if (args.size != 1) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が誤っています。")
                            return@execute
                        }

                        if (gameManager.runningGames[args[1]] == null) {
                            player.sendMessage("${prefix}${ChatColor.RED}ゲームが存在しません。")
                            return@execute
                        }

                        gameManager.viewTeleport(args[1], player)
                    }

                    "return" -> {

                        if (!player.hasPermission("mb.view")) {
                            player.sendMessage("${prefix}${ChatColor.RED}権限がありません。")
                            return@execute
                        }

                        if (player.world.name != "bookmaker") {
                            player.sendMessage("${prefix}${ChatColor.RED}あなたは観戦していません。")
                            return@execute
                        }

                        player.teleport(Location(Bukkit.getWorld("bookmaker"), 0.0, 100.0, 0.0))

                    }

                    "open" -> {

                        if (isLocked) {
                            player.sendMessage("${prefix}${ChatColor.RED}ブックメーカーは現在OFFになっています。")
                            return@execute
                        }

                        if (args.size != 2) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が誤っています。")
                            return@execute
                        }

                        gameManager.openNewGame(args[1], player)

                        return@execute

                    }

                    "reload" -> {

                        if (!player.hasPermission("mb.op")) {
                            player.sendMessage("${prefix}${ChatColor.RED}権限がありません。")
                            return@execute
                        }

                        configManager.loadSpawn(player)
                        configManager.loadConfig(player)

                        return@execute

                    }

                    "list" -> {

                        if (!player.hasPermission("mb.op")) {
                            player.sendMessage("${prefix}${ChatColor.RED}権限がありません。")
                            return@execute
                        }

                        player.sendMessage("${prefix}${ChatColor.GOLD}現在${gameManager.loadedGames.size}個のゲームがロードされています。")
                        for (item in gameManager.loadedGames) {
                            player.sendMessage(prefix + item.key + " (" + item.value.gameName + ")")
                        }

                    }

                    "info" -> {

                        if (!player.hasPermission("mb.op")) {
                            player.sendMessage("${prefix}${ChatColor.RED}権限がありません。")
                            return@execute
                        }

                        if (args.size != 2) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が間違っています。")
                            return@execute
                        }

                        if (gameManager.loadedGames[args[1]] == null) {
                            player.sendMessage("${prefix}${ChatColor.RED}指定されたゲームは存在しません。")
                            return@execute
                        }

                        //FLAG
                        var checkingGame: Game = gameManager.loadedGames[args[1]]!!
                        player.sendMessage("${prefix}${ChatColor.GOLD}${args[1]}のデータ")
                        player.sendMessage("${prefix}${ChatColor.GRAY}ゲーム名: ${checkingGame.gameName}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}GUI表示アイテム: ${checkingGame.item}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}プレイヤー人数: ${checkingGame.playerNumber}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}参加費: ${checkingGame.joinFee}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}税率: ${checkingGame.tax}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}賞金率: ${checkingGame.prize}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}ステータス: ${checkingGame.status}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}参加登録者一覧: ${checkingGame.candidates}")
                        player.sendMessage("${prefix}${ChatColor.GRAY}ベット一覧: ${checkingGame.players}")

                    }

                    "push" -> {

                        if (args.size != 2) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が間違っています。")
                            return@execute
                        }

                        gameManager.pushPhase(args[1], player)

                    }

                    "end" -> {
                        if (args.size != 3) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が間違っています。")
                            return@execute
                        }

                        if (gameManager.runningGames[args[1]] == null) {
                            player.sendMessage("${prefix}${ChatColor.RED}指定されたゲームが存在しません。")
                            return@execute
                        }
                    }

                    "forcestop" -> {
                        if (!gameManager.UUIDMap.keys.contains(gameManager.runningGames[args[1]]!!.players.keys.toList()[0])) {
                            player.sendMessage("${prefix}${ChatColor.RED}プレイヤーが存在しません。")
                            return@execute
                        }

                        if (args[2].toIntOrNull() != null) {
                            sender.sendMessage("${prefix}${ChatColor.RED}選択肢の番号を入力してください。")
                            return@execute
                        }

                        if (args[2].toInt() == 1 || args[2].toInt() == 2) {
                            gameManager.endGame(args[1], gameManager.runningGames[args[1]]!!.players.keys.toList()[args[2].toInt() - 1])
                        }

                        return@execute

                    }

                    "setfighterspawn" -> {

                        if (args.size != 2) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が間違っています。")
                            return@execute
                        }

                        if (gameManager.loadedGames[args[1]] != null) {
                            gameManager.setFighterSpawnPoint(args[1], player)
                        }

                        return@execute

                    }

                    "setviewerspawn" -> {

                        if (args.size != 2) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が間違っています。")
                            return@execute
                        }

                        if (gameManager.loadedGames[args[1]] != null) {
                            gameManager.setViewerSpawnPoint(args[1], player)
                        }

                        return@execute

                    }

                    "off" -> {

                        isLocked = true
                        player.sendMessage("${prefix}OFFにしました。")

                        return@execute

                    }
                    "on" -> {

                        isLocked = false
                        player.sendMessage("${prefix}ONにしました。")

                        return@execute

                    }

                    "ask" -> {

                        if (args.size != 5) {
                            player.sendMessage("${prefix}${ChatColor.RED}引数が間違っています。")
                            return@execute
                        }

                        if (gameManager.loadedGames[args[1]] == null && gameManager.runningGames[args[1]] != null) {
                            player.sendMessage("${prefix}${ChatColor.RED}ゲームがすでに存在します。")
                            return@execute
                        }
                            gameManager.openNewQ(args[1], args[2], args[3], args[4])

                    }

                }

            }

        }

    }

    fun showHelp(sender: CommandSender) {
        sender.sendMessage("§f§l=====( §a§lm§6§lBookMaker§f§l )=====")
        sender.sendMessage("§6《データー管理系》")
        sender.sendMessage("§a/mb reload §7config.ymlとregionをリロードする")
        sender.sendMessage("§a/mb list §7登録中のゲームを表示する")
        sender.sendMessage("§a/mb info <ゲームid> §7指定したゲームの情報を表示する")
        sender.sendMessage(" ")
        sender.sendMessage("§6《ゲーム管理系》")
        sender.sendMessage("§a/mb open <ゲームid> §7指定したゲームを開く (一般人使用可能)")
        sender.sendMessage("§a/mb ask <新ゲームid> <質問> <選択肢1> <選択肢2> §7ASKモードの試合をオープンする。")
        sender.sendMessage("§a/mb forcestop <ゲームid> §7指定したゲームを強制終了する")
        sender.sendMessage("§a/mb push <ゲームid> §7指定したゲームを次のフェーズに進ませる")
        sender.sendMessage("§a/mb end <ゲームid> <勝者> §7試合中のゲームを終了させる")
        sender.sendMessage("§a/mb off §7ブックメーカーをロックする。")
        sender.sendMessage("§a/mb on §7ブックメーカーをアンロックする。")
        sender.sendMessage("§a/mb view <ゲームid> §7観戦場所にtpする (一般人使用可能)")
        sender.sendMessage("§a/mb view <ゲームid> §7ブックメーカーロビーにtpする (一般人使用可能)")
        sender.sendMessage("")
        sender.sendMessage("§6《ポイント管理系》")
        sender.sendMessage("§a/mb setfighterspawn <ゲームid> §7立っているところを選手のスポーンポイントにする")
        sender.sendMessage("§a/mb setviewerspawn <ゲームid> §7立っているところを選手のスポーンポイントにする")
        sender.sendMessage(" ")
        sender.sendMessage("§6Ver 1.0  Made by Shupro")
        sender.sendMessage("§f§l=====================")
    }

//    fun fixTpBug(tpedPlayer: Player) {
//        for (player in Bukkit.getWorld(worldName).getPlayers()) {
//            tpedPlayer.hidePlayer(player)
//            player.hidePlayer(tpedPlayer)
//        }
//        for (player in Bukkit.getWorld(worldName).getPlayers()) {
//            tpedPlayer.showPlayer(player)
//            player.showPlayer(tpedPlayer)
//        }
//    }
}
