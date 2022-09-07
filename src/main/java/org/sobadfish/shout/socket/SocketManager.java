package org.sobadfish.shout.socket;



import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Socket 通信控制
 * @author Sobadfish
 *
 * */
public class SocketManager {


    // 线程池管理
    public static ExecutorService executor = Executors.newCachedThreadPool() ;

    private SocketNode socket;

    /*
    是否为启动状态
    * */
    public boolean enable;

    /**
     * 主机分配的端口
     * */
    private static int port = -1;

    private final SocketType type;

    private SocketDataListener dataListener;

    private SocketConnectListener connectListener;

    private ServerSocket serverSocket;

    private List<SocketNode> sockets = new CopyOnWriteArrayList<>();

    private SocketManager(SocketNode socket){
        this.socket = socket;
        type = SocketType.SOCKET;
        enable = true;
        connect();

    }


    private SocketManager(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
        this.sockets = new CopyOnWriteArrayList<>();
        port = serverSocket.getLocalPort();
        type = SocketType.SERVER;
        enable();
        enable = true;
        connect();


    }

    public SocketType getType() {
        return type;
    }

    /**
     * 绑定数据监听 网络通信中获取到的数据将通过监听事件发送
     * @param listener 事件监听
     * */
    public void setDataListener(SocketDataListener listener) {
        this.dataListener = listener;
    }

    /**
     * 建立服务端与客户端的通信 如果服务端不存在，则创建服务端
     *
     * @param host 服务器IP
     * @param port 服务器端口
     * */
    public static SocketManager connectManager(String host, int port){
        try {
            Socket socket = new Socket(host, port);
            SocketNode node = SocketNode.getNode(socket);
            if(node != null){
                return new SocketManager(node);
            }else{
                return null;
            }

        } catch (Exception e) {
            // 不存在服务器
            System.out.println("不存在服务主机 或 主机无法连接 " + host + " 正在创建端口 " + port + " 的主机");
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                return new SocketManager(serverSocket);
            } catch (IOException ioException) {

                System.out.println("创建失败 " + ioException.getMessage());

            }

        }
        return null;
    }

    /**
     * 绑定连接事件监听
     * 当有 Socket 连接的时候会通过这个事件回调
     *
     * @param listener 监听器
     * */
    public void setConnectListener(SocketConnectListener listener) {
        this.connectListener = listener;
    }

    /**
     * 向网络中发送数据
     * @param messageData 监听器
     * */
    public boolean sendMessage(MessageData messageData){
        switch (type){
            case SOCKET:
                if(socket != null && port > 0){
                    // 通信建立后才行发送数据
                    socket.sendMessage(messageData);
                    return true;
                }
                break;
            case SERVER:
                for(SocketNode node: sockets){
                    node.sendMessage(messageData);
                    return true;
                }
                break;
            default:break;
        }
        return false;
    }

    private void enable() {
        executor.execute(() -> {
            Socket socket = null;
            while (serverSocket != null && !serverSocket.isClosed()){
                try {
                    socket = serverSocket.accept();
                    SocketNode node = SocketNode.getNode(socket);
                    if(node != null){
                        sockets.add(node);
                        // 回传分配的端口
                        node.sendMessage(MessageData.createMessage(new PortBack(node.port)));
                        if(connectListener != null){
                            connectListener.join(node);
                        }
                    }
                } catch (IOException e) {
                    if(socket != null){
                        System.out.println(socket.getInetAddress()+":"+socket.getPort()+"断开连接");
                    }

                }
            }
        });
    }


    /**
     *
     * 数据通信类
     * 用于向网络发送数据
     * */
    public static class MessageData{

        public String host;

        public int port;

        public String msg;

        public Long time;


        public static MessageData createMessage(Object o){
            MessageData data = new MessageData();
            try {
                InetAddress addr = InetAddress.getLocalHost();
                data.host = addr.getHostAddress();

            } catch (UnknownHostException e) {
                data.host = "Unknown Host";
            }
            data.time = System.currentTimeMillis();
            Gson gson = new Gson();
            data.msg = gson.toJson(o);
            return data;
        }

        public <T> T getData(Class<T> data){
            Gson gson = new Gson();
            return gson.fromJson(msg,data);
        }
    }



    public static class PortBack{
        public int port;
        public PortBack(int port){
            this.port = port;
        }

        public int getPort() {
            return port;
        }
    }




    private void connect() {
        executor.execute(() -> {
            while (isEnable()) {
                if (type == SocketType.SERVER) {
                    // 遍历时修改
                    for (SocketNode node : sockets) {
                        if (node != null && node.isConnected()) {
                            if(node.isEnable()){
                                if(!node.read(this)){
                                    if(connectListener != null){
                                        connectListener.quit(node);
                                    }
                                    node.disable();
                                }
                            }
                        }
                    }
                }
                if (type == SocketType.SOCKET) {
                    if (socket != null && socket.isConnected()) {
//                        read(socket.socket);
                        if(socket.isEnable()){
                            if(!socket.read(this)){
                                if(connectListener != null){
                                    connectListener.quit(socket);
                                }
                                socket.disable();

                            }
                        }
                    }
                }
            }

        });
    }


    /**
     * 判断连接是否有效
     *
     * */
    public boolean isEnable(){
        if(enable) {
            switch (type) {
                case SERVER:
                    if (serverSocket != null) {
                        return !serverSocket.isClosed();
                    }
                    break;
                case SOCKET:
                    if (socket != null) {
                        return socket.isConnected();
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    public enum SocketType{
        /**
         * 类型为主机*/
        SERVER,
        /**
         * 类型为客户端*/
        SOCKET
    }

    public interface SocketDataListener{

        void handleMessage(SocketManager socketManager,MessageData messageData);
    }

    public interface SocketConnectListener{

        void join(SocketNode socket);

        void quit(SocketNode socket);
    }

    public void sendMessage(Object o){
        MessageData messageData = MessageData.createMessage(o);
        sendMessage(messageData);

    }


    public static class SocketNode{

        private Socket socket;

        private final String ip;


        private boolean enable;

        private final int port;

        public String getIPAddress(){
           return ip;
        }

        public int getPort() {
            return port;
        }

        private SocketNode(Socket socket){
            this.socket = socket;
            enable = true;
            if(socket != null && socket.isConnected()){
                ip = socket.getInetAddress().getHostAddress();
                port =  socket.getPort();
                try {
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                } catch (IOException e) {
                    inputStream = null;
                    outputStream = null;
                }
            }else{
                ip = "Unknown";
                port = -1;
            }


        }

        public boolean isEnable() {
            return enable;
        }

        public void disable(){
            enable = false;
            if(socket != null){
                try {
                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean read(SocketManager manager){

            if(inputStream == null){
                return false;
            }
            Gson gson = new Gson();
            while (!socket.isClosed()){
                try {
                    byte[] bytes = new byte[1024];
                    inputStream.read(bytes,0,bytes.length);
                    String out = new String(bytes, StandardCharsets.UTF_8).trim();
                    if(!out.trim().equalsIgnoreCase("") ){

                        MessageData data = gson.fromJson(out, MessageData.class);
                        PortBack portBack = data.getData(PortBack.class);
                        if (portBack != null) {
                            SocketManager.port = portBack.port;
                            if (manager.connectListener != null) {
                                manager.connectListener.join(this);
                            }
                        }
                        // 判断是否为主机 如果是主机就把收到的数据分发给其他客户端
                        if (manager.type == SocketType.SERVER) {
                            for (SocketNode node : manager.sockets) {
                                // 别再把数据又发回去了，这不就重复了吗
                                //当然你也可以选择重复，只在接收区显示
                                //增加这个判断是为了 忽略发送数据到服务器的客户端
                                if (node.equals(this)) {
                                    continue;
                                }
                                node.sendMessage(data);
                            }
                        }
                        if (manager.dataListener != null) {
                            manager.dataListener.handleMessage(manager, data);
                        }

                    }

                }catch (Exception e){
                    return false;
                }
            }
            if(socket.isClosed()){
                // 进入这个判断后说明连接被关闭了
                switch (manager.type){
                    case SERVER:
                        if(manager.connectListener != null){
                            manager.connectListener.quit(this);
                        }
                        return false;
                    case SOCKET:
                        // 尝试重连主机
                        int error = 0;
                        while (error < 3){
                            try {
//
                                socket = new Socket(socket.getInetAddress(), socket.getPort());
                                // 重连完成
                                break;
                            }catch (Exception e) {
                                error++;
                            }
                        }
                        if(error >= 3){
                            return false;
                        }

                        break;
                }
            }
            return true;

        }


        public static SocketNode getNode(Socket socket){
            if(socket.isConnected()){
                return new SocketNode(socket);
            }
            return null;
        }


        public boolean isConnected() {
            return socket != null && socket.isConnected();
        }

        private void sendMessage(MessageData messageData){
            if(isConnected()) {
                messageData.port = port;
                Gson gson = new Gson();
                byte[] msg = gson.toJson(messageData).getBytes();
                try {
                    if(outputStream != null){
                        outputStream.write(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        private InputStream inputStream;

        private OutputStream outputStream;



        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SocketNode node = (SocketNode) o;
            return Objects.equals(socket, node.socket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(socket);
        }
    }

    public void disable(){
        enable = false;
        if(serverSocket != null){
            try {
                serverSocket.close();
                serverSocket = null;
                sockets.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(socket != null){
            try {
                socket.socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
