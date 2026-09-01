package Main;

import java.awt.Image;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/** Shared Khalid Farms logo helpers. */
public final class ShopBranding {

    public static final String LOGO_RESOURCE = "/Main/image/khalid_farms_logo.jpg";
    public static final String DEFAULT_LOGO_FILE = "data/shop_logo.jpg";

    private ShopBranding() {
    }

    public static URL logoUrl() {
        return ShopBranding.class.getResource(LOGO_RESOURCE);
    }

    public static ImageIcon logoIcon(int size) {
        URL url = logoUrl();
        if (url == null) {
            return null;
        }
        ImageIcon icon = new ImageIcon(url);
        Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    public static void applyLogoLabel(JLabel label, int size) {
        ImageIcon icon = logoIcon(size);
        if (icon == null) {
            return;
        }
        label.setIcon(icon);
        label.setText("");
        label.setOpaque(false);
    }
}
