package modelPGAS;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Class that encapsulates network address info
 *   
 * @author adrian
 *
 */
public class IPAddress {

    private InetAddress ip; // IP address
    private int portUDP; // UDP port

    /**
     * Constructor
     * @param ip
     * @param portUDP
     */
    public IPAddress(String ip, int portUDP) {
        try {
            this.ip = InetAddress.getByName(ip);
            this.portUDP = portUDP;
        } catch (UnknownHostException ex) {
            // Todo
        }
    }

    /**
     * Get IP address
     * @return the IP address
     */
    public InetAddress getIp() {
        return ip;
    }

    /**
     * Set IP address
     */
    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    /**
     * Get UDP port
     * @return UDP port
     */
    public int getPortUDP() {
        return portUDP;
    }

    /**
     * Set UDP port
     */
    public void setPortUDP(int portUDP) {
        this.portUDP = portUDP;
    }


    /**
     * Method toString
     */
    @Override
    public String toString() {
        return "ip:" + getIp().getHostAddress() + " UDP port:" + getPortUDP();
    }

}
