# wifihotspot
使用WiFi热点实现简单的通信

WiFi(Wireless Fidelity)又称802.11b标准,是IEEE定义的一个无线网络通信的工业标准。该技术使用的是2.4GHz附近的频段。
Wi-Fi主要特征如下
1.速度快，最高宽带为11Mbps
2.可靠性强，在信号较弱或者有干扰的情况下，宽带可自动调整为5.5Mbps、2Mbps或者1Mbps，有效的保障了网络的稳定性和可靠性。
3.距离较远，在开放性区域，通信距离可达305米，在封闭性区域，通信距离可达100米左右。
4.方便与现有的有线以太网整合，组网的成本更低。

Android Wi-Fi相关类
Android中Wi-Fi按照层次结构设计的，如下所示。

        ------------------             ---------------            --------------------
       | WirelessSetting |             | WifiSetting |-----------+| AccessPointDialog|
       -------------------             ---------------            --------------------
               |                             |     |                        |
               ------------------------------       --------------          |  
                            |                                    |          |
                     -------+-------                       ------+------    |
                 |--+| WifiEnabler |     ------------------| WifiLayer |+----
                 |   ---------------     |                 ------+---+--  
                 |----------|------------|-----------------------|   |------------- NETWORK_STATE_CHANGED_ACTION
      WIFI_STATE_|CHANGED_AC|TIO         |                                        | SCAN_RESULTS_AVAILABLE_ACTION
                 |   -------+-------     |                 --------------------   | SUPPLICANT_CONNECTION_CHANGE_ACTION
                 |   | WifiManager |+----|  |-------------+| WifiState Tracker|----
                 |   ---------------        |              --------+-----------
                 |          |               |                      |
                 |   -------+-------        |              --------+------
                 ----| WifiService |---------              | WifiMonitor |
                     -------+-------                       ---------------
                            |                                     |
                            |-----------------|-------------------- 
                                      --------+--------
                                      |   WifiNative  |                JAVA VM
                                      -----------------
        ______________________________________|_________________________________________
                                              |JNI
                                   -----------+------------
                                   | android_net_wifi_wifi|
                                   ------------------------
                                              |
                                              |
                                       -------+------
                                       |    wifi    |
                                       --------------
                                              | Socket
                                    ----------+-------
                                    | wpa_supplicant |
                                    ------------------
                                    
Android Wi-Fi模块自下向上可以分为5层：硬件驱动程序、wpa_supplication、JNI、WiFi API、WifiSetting应用程序。
1.wpa_supplicant是一个开源库，是Android实现Wi-Fi功能的基础。它从上层接到命令后，通过Socket与硬件驱动进行通信，操作硬件完成需要的操作。
2.JNI(Java Native Interface)实现了Java代码与其他代码的交互，使得在Java虚拟机中运行的Java代码能够与其他语言编写的应用程序和库进行交互。
在Android中，JNI可以让Java程序调用C程序。
3.Wi-Fi API使应用程序可以使用WiFi功能。
4.Wifi Settings应用程序是Android中自带的一个应用程序，选择手机的Settings->Wireless&networks->Wi-Fi,可以让用户手动打开或关闭Wi-Fi功能，
当用户打开Wi-Fi功能后，它会自动搜索周围的无线网络，并以列表的形式显示，供用户选择，默认会连接用户上一次成功连接的无线网络。

在android.net.wifi包中提供了一些类管理设备的Wi-Fi功能，主要包括ScanResult、wifiConfiguration、WifiInfo和WifiManager.