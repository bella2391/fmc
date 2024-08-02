package spigot;

import com.google.inject.Guice;
import com.google.inject.Injector;

import spigot_command.FMCCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Main
{
	private static Injector injector = null;
	
	public Connection conn = null;
	public PreparedStatement ps = null;
	public final common.Main plugin;
	
	public Main(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	public void onEnable()
    {
		// Guice インジェクターを作成
        injector = Guice.createInjector(new SpigotModule(plugin, this));
        
		plugin.getLogger().info("Detected Spigot platform.");
		
		getInjector().getInstance(AutoShutdown.class).startCheckForPlayers();
		
	    plugin.saveDefaultConfig();
		
    	plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(EventListener.class), plugin);
        
    	plugin.getCommand("fmc").setExecutor(getInjector().getInstance(FMCCommand.class));
        
    	if(plugin.getConfig().getBoolean("MCVC.Mode",false))
		{
    		getInjector().getInstance(Rcon.class).startMCVC();
		}
    	
    	getInjector().getInstance(DoServerOnline.class).UpdateDatabase();
    	
    	plugin.getLogger().info("プラグインが有効になりました。");
    }
    
	public static Injector getInjector()
    {
        return injector;
    }
	
    public void onDisable()
    {
    	if(plugin.getConfig().getBoolean("MCVC.Mode",false))
		{
    		getInjector().getInstance(Rcon.class).stopMCVC();
		}
        
    	getInjector().getInstance(AutoShutdown.class).stopCheckForPlayers();
        
    	plugin.getLogger().info("Socket Server stopping...");
    	
    	getInjector().getInstance(DoServerOffline.class).UpdateDatabase();
    	
    	plugin.getLogger().info("プラグインが無効になりました。");
    }
}
