/*
 * Mod ported to Forge/FML and modified by adamros
 */

package adamros.ccftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Property;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import de.doridian.ccftp.ComputerCraftFTP;

@Mod(modid="CCFTP", name="ComputerCraftFTP", version="1.1")
public class BaseCCFTP {

	@Instance("CCFTP")
	public static BaseCCFTP instance;
	
	public static Configuration config;
	
	private Property property;
	
	public static String serverIP;
	public static int serverPort;
	public static String serverPassivePorts;
	public static String serverPassiveLocalAddress;
	public static String maxFileSize;
	public static String defaultWorld;
	public static String detectScript;
	
	@PreInit
	public void preInit(FMLPreInitializationEvent event)
	{
		if (!isServerSide())
			throw new RuntimeException("This mod is server-side only! Can't run it on client-side!");
		
		instance = this;
		config = new Configuration(event.getSuggestedConfigurationFile());
		
		try {
			config.load();
		}
		catch (Exception e)
		{
			FMLLog.getLogger().severe("Cannot load configuration file: " + e.getMessage());
		}
	}
	
	@Init
	public void init(FMLInitializationEvent event)
	{
		property = config.get("basic", "server-ip", "0.0.0.0");
		serverIP = property.getString();
		
		property = config.get("basic", "server-port", 2221);
		serverPort = property.getInt();
		
		property = config.get("basic", "server-passive-ports", "40000-50000");
		serverPassivePorts = property.getString();
		
		property = config.get("basic", "server-passive-local-address", "0.0.0.0");
		serverPassiveLocalAddress = property.getString();
		
		property = config.get("basic", "default-world", "world");
		defaultWorld = property.getString();
		
		property = config.get("limits", "max-file-size", "1MB");
		maxFileSize = property.getString();
		
		config.getCategory("advanced").setComment("IMPORTANT! Don't change anything here until you know what are you doing! It will work without touching this section!");
		
		property = config.get("advanced", "check-script-url", "http://lixium.pl/ipcheck.php");
		detectScript = property.getString();
		
		config.save();
		
	}
	@PostInit
	public void postInit(FMLPostInitializationEvent event)
	{
		new Thread() {
			@Override
			public void run() {
				while(FMLCommonHandler.instance().getMinecraftServerInstance().getNetworkThread() == null)
				{
					try {
						Thread.sleep(1000);
					}
					catch (Exception e)
					{
						FMLLog.getLogger().info("Waiting for network thread");
					}
					
					if (serverPassiveLocalAddress == "" || serverPassiveLocalAddress.isEmpty() || serverPassiveLocalAddress == "0.0.0.0")
					{
						String addr = "0.0.0.0";
						try {
							Socket sock = new Socket("8.8.8.8", 53);
	                        addr = sock.getLocalAddress().getHostAddress();
	                        sock.close();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
						
						URLConnection urlConnection;
						
						try {
							urlConnection = new URL(detectScript).openConnection();
							urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
							BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
	                        String newAddr = reader.readLine();
	                        
	                        if(!newAddr.equalsIgnoreCase(addr)) {
	                            serverPassiveLocalAddress = newAddr;
	                            FMLLog.getLogger().info("Autodetected passive local address to be: " + newAddr);
	                        }
	                        
	                        reader.close();
						}
						catch (MalformedURLException e)
						{
							e.printStackTrace();
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
						
						FMLLog.getLogger().fine("Starting CCFTPd on " + serverIP + ":" + serverPort + " (default world \"" + defaultWorld + "\", using passive ports " + serverPassivePorts + ")");
						
						try {
							new ComputerCraftFTP(serverPort, serverIP, serverPassivePorts, serverPassiveLocalAddress, maxFileSize);
						}
						catch (Exception e)
						{
							FMLLog.getLogger().severe("Starting CCFTPd failed! Stack trace: ");
							e.printStackTrace();
						}
					}
				}
			}
		}.start();
	}
	
	public boolean isServerSide()
	{
		if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER || FMLCommonHandler.instance().getEffectiveSide() == Side.BUKKIT)
		{
			return true;
		}
		
		return false;
	}
}
