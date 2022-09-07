package org.sobadfish.shout;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import com.google.common.base.Throwables;
import com.google.gson.Gson;
import org.sobadfish.shout.configs.MsgConfig;
import org.sobadfish.shout.configs.ShoutConfig;
import org.sobadfish.shout.from.WindowsFrom;
import org.sobadfish.shout.money.MoneyManager;
import org.sobadfish.shout.socket.SocketManager;
import org.sobadfish.shout.utils.TextUtils;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 _   _   _   _____   _   _   _____   _   _   _____
| | | | | | /  ___/ | | | | /  _  \ | | | | |_   _|
| | | | | | | |___  | |_| | | | | | | | | |   | |
| | | | | | \___  \ |  _  | | | | | | | | |   | |
| |_| | | |  ___| | | | | | | |_| | | |_| |   | |
\_____/ |_| /_____/ |_| |_| \_____/ \_____/   |_|
*
* */
public class ShoutPlugin extends PluginBase implements Listener {

    public static final String TITLE = "&7[&6UI-SHOUT&7] ";

    private static ShoutPlugin shoutPlugin;

    private MsgConfig msgConfig;

    public SocketManager socketManager;

    public MoneyManager moneyManager;

    private ShoutConfig shoutConfig;

    private WindowsFrom windowsFrom;

    @Override
    public void onEnable() {
        this.getLogger().info("正在加载全服喊话");
        shoutPlugin = this;
        saveDefaultConfig();
        reloadConfig();
        getConfig();
        windowsFrom = new WindowsFrom();
        this.getServer().getPluginManager().registerEvents(windowsFrom,this);
        this.getLogger().info(TextFormat.AQUA+" _   _   _   _____   _   _   _____   _   _   _____  ");
        this.getLogger().info(TextFormat.AQUA+"| | | | | | /  ___/ | | | | /  _  \\ | | | | |_   _| ");
        this.getLogger().info(TextFormat.AQUA+"| | | | | | | |___  | |_| | | | | | | | | |   | |   ");
        this.getLogger().info(TextFormat.AQUA+"| | | | | | \\___  \\ |  _  | | | | | | | | |   | |   ");
        this.getLogger().info(TextFormat.AQUA+"| |_| | | |  ___| | | | | | | |_| | | |_| |   | |   ");
        this.getLogger().info(TextFormat.AQUA+"\\_____/ |_| /_____/ |_| |_| \\_____/ \\_____/   |_|   \n\n");
        this.getLogger().info("初始化配置文件");
        initConfig();
        this.getLogger().info("启动检查网络环境");
        initNetWork();

        moneyManager = MoneyManager.getManager();

    }

    private void initConfig() {
        saveResource("msg.json",false);
        saveResource("shout.json",false);
        msgConfig = fileToClass(new File(this.getDataFolder()+"/msg.json"),MsgConfig.class);
        shoutConfig = fileToClass(new File(this.getDataFolder()+"/shout.json"),ShoutConfig.class);
    }

    public void setMoneyManager(MoneyManager moneyManager) {
        this.moneyManager = moneyManager;
    }

    public static ShoutPlugin getShoutPlugin() {
        return shoutPlugin;
    }

    public MsgConfig getMsgConfig() {
        return msgConfig;
    }

    public ShoutConfig getShoutConfig() {
        return shoutConfig;
    }

    public void initNetWork(){

        String host = getConfig().getString("server.host","127.0.0.1");
        int port = getConfig().getInt("port",25633);
        socketManager = SocketManager.connectManager(host,port);
        if(socketManager != null){
            this.getLogger().info("网络环境检查完成 当前连接类型为 "+TextFormat.colorize('&',"&e"+socketManager.getType()+""));
            socketManager.setConnectListener(new SocketManager.SocketConnectListener() {
                @Override
                public void join(SocketManager.SocketNode socket) {
                    getLogger().info(socket.getIPAddress()+":"+socket.getPort()+" 已连接");
                }

                @Override
                public void quit(SocketManager.SocketNode socket) {
                    getLogger().info(socket.getIPAddress()+":"+socket.getPort()+" 已断开");

                }
            });
            socketManager.setDataListener((socketManager, messageData) -> {
                MsgBroadcastData data = messageData.getData(MsgBroadcastData.class);
                broadcastMessage(messageData,data.type);
            });
        }



    }



    public static class MsgBroadcastData{
        public String msg;

        public String world;

        public String player;

        public String type;
    }

    public void broadcastMessage(SocketManager.MessageData msg, String type){
        MsgBroadcastData data = msg.getData(MsgBroadcastData.class);
        String display = ShoutPlugin.getShoutPlugin().getConfig().getString("msg");
        display = display.replace("${ip}",msg.name)
                .replace("${name}",data.player)
                .replace("${world}",data.world)
                .replace("${msg}", data.msg);
        final String s = display;
        switch (type.toLowerCase()){
            case "msg":
                Server.getInstance().broadcastMessage(TextFormat.colorize('&',s));
                break;
            case "tip":
                Server.getInstance().getOnlinePlayers().values().forEach(player1 -> player1.sendTip(TextFormat.colorize('&',s)));
                break;
            case "popup":
                Server.getInstance().getOnlinePlayers().values().forEach(player1 -> player1.sendPopup(TextFormat.colorize('&',s)));
                break;
            case "title":
                Server.getInstance().getOnlinePlayers().values().forEach(player1 -> player1.sendTitle(TextFormat.colorize('&',s)));
                break;
            default:break;
        }
    }

    private <T> T fileToClass(File file,Class<T> tClass) {
        Gson gson = new Gson();
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            return gson.fromJson(reader,tClass);
        } catch (IOException e) {
            this.getLogger().error("无法读取 "+file.getName()+" 配置文件");
            e.printStackTrace();
        }finally {
            if(reader !=null){
                try {
                    reader.close();
                } catch (IOException e) {
                    this.getLogger().error(Throwables.getStackTraceAsString(e));
                }
            }
        }
        return null;
    }


    @Override
    public void onDisable() {
        if(socketManager != null){
            socketManager.disable();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.isPlayer()){

            windowsFrom.displayWindow((Player)sender,shoutConfig);
            windowsFrom.setFromDataListener((player, data) -> {
                String msg = TextFormat.colorize('&',data.msg);
                if("无色彩".equalsIgnoreCase(data.color)){
                    msg = TextUtils.clearColor(msg);
                }
                float money = shoutConfig.rate.msg * TextUtils.mathLine(msg);
                switch (data.color.toLowerCase()){
                    case "自定义":
                        money *= shoutConfig.rate.custom;
                        break;
                    case "§c随§6机§e颜§a色":
                        money *= shoutConfig.rate.random;
                        msg = TextUtils.roundColor(msg);
                        break;
                    default:break;
                }
                money *= shoutConfig.money;
                if(moneyManager.myMoney(player.getName()) > money){
                    moneyManager.reduceMoney(player.getName(),money);
                    player.sendMessage(TextFormat.colorize('&',TITLE+" &2成功花费 &7"+money+" &2进行了一次喊话"));
                }else{
                    player.sendMessage(TextFormat.colorize('&',TITLE+" &4您的金钱不足"));
                }
               sendMessage(player,msg,data.type);

            });

        }else{
            sendMessage(sender,args[0],"msg");
        }
        return super.onCommand(sender, command, label, args);
    }

    public void sendMessage(CommandSender player,String msg,String type){
        MsgBroadcastData data1 = new MsgBroadcastData();
        data1.msg = msg;
        data1.world = ShoutPlugin.getShoutPlugin().getConfig().getString("server.name");
        if(player instanceof Player){
            data1.world = ((Player) player).getLevel().getFolderName();
        }
        data1.player = player.getName();
        data1.type = type;
        if(socketManager != null){
            socketManager.sendMessage(data1);

        }
        if(socketManager == null || !socketManager.enable || socketManager.getType() == SocketManager.SocketType.SERVER){
            SocketManager.MessageData mdata = new SocketManager.MessageData();
            Gson gson = new Gson();
            mdata.name = getConfig().getString("server.name");
            mdata.msg = gson.toJson(data1);
            broadcastMessage(mdata,type);
        }
    }


}


