package io.le.proxy.client.gui;

import io.le.proxy.client.gui.settings.Settings;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TrayGUI {

    private TrayIcon trayIcon;
    private SystemTray tray;
    private AtomicBoolean visible = new AtomicBoolean(false);

    public TrayGUI(Settings settings) {
        if(! SystemTray.isSupported()) {
            log.warn("The current system does not support trays!");
            return;
        }

        try {
            // 获得本操作系统托盘的实例
            tray = SystemTray.getSystemTray();

            // 构造一个右键弹出式菜单
            PopupMenu pop = new PopupMenu();
            // MenuItem about = new MenuItem("关于我们");
            // pop.add(about);
            MenuItem exit = new MenuItem("Exit");
            pop.add(exit);

            // 显示在托盘中的图标
            ImageIcon icon = new ImageIcon(getClass().getResource("/favicon.png"));
            trayIcon = new TrayIcon(icon.getImage(), "Proxy", pop);
            // 这句很重要，没有会导致图片显示不出来
            trayIcon.setImageAutoSize(true);
            // 监听鼠标
            trayIcon.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Double click
                        settings.setVisible(!settings.isVisible());
                        remove();
                    }

                }
            });

            exit.addActionListener(e -> {
                if(e.getSource() == exit) {
                    setToolTip("Exiting...");
                    System.exit(0);
                }
            });
            log.debug("Create system tray successfully");
        } catch (Exception e) {
            log.error("Create system tray exception", e);
        }
    }

    public void setToolTip(String tooltip) {
        trayIcon.setToolTip(tooltip);
    }

    public void add() {
        try {
            if(tray != null && visible.compareAndSet(false, true)) {
                tray.add(trayIcon);
            }
        } catch (AWTException e) {
            log.error("Add system tray exception", e);
        }
    }

    public void remove() {
        if(tray != null) {
            if(visible.compareAndSet(true, false)) {
                tray.remove(trayIcon);
            }
        }
    }
}
