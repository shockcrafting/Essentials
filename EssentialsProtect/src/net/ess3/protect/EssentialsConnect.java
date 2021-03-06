package net.ess3.protect;

import static net.ess3.I18n._;
import net.ess3.api.IEssentials;
import net.ess3.api.IReload;
import net.ess3.api.IUser;
import net.ess3.protect.data.ProtectedBlockMySQL;
import net.ess3.protect.data.ProtectedBlockSQLite;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


public class EssentialsConnect
{
	private static final Logger LOGGER = Logger.getLogger("Minecraft");
	private final transient IEssentials ess;
	private final transient IProtect protect;

	public EssentialsConnect(final Plugin essPlugin, final Plugin essProtect)
	{
		if (!essProtect.getDescription().getVersion().equals(essPlugin.getDescription().getVersion()))
		{
			LOGGER.log(Level.WARNING, _("versionMismatchAll"));
		}
		ess = (IEssentials)essPlugin;
		protect = (IProtect)essProtect;
		protect.setSettings(new ProtectHolder(ess));
		final ProtectReloader pr = new ProtectReloader();
		pr.onReload();
		ess.addReloadListener(pr);
	}

	public IEssentials getEssentials()
	{
		return ess;
	}

	public void alert(final Player user, final String item, final String type)
	{
		final Location loc = user.getLocation();
		final String warnMessage = _("alertFormat", user.getName(), type, item,
									 loc.getWorld().getName() + "," + loc.getBlockX() + ","
									 + loc.getBlockY() + "," + loc.getBlockZ());
		LOGGER.log(Level.WARNING, warnMessage);
		for (Player p : ess.getServer().getOnlinePlayers())
		{
			final IUser alertUser = ess.getUserMap().getUser(p);
			if (Permissions.ALERTS.isAuthorized(alertUser))
			{
				alertUser.sendMessage(warnMessage);
			}
		}
	}


	private class ProtectReloader implements IReload
	{
		@Override
		public void onReload()
		{
			if (protect.getStorage() != null)
			{
				protect.getStorage().onPluginDeactivation();
			}

			/*
			 * for (ProtectConfig protectConfig : ProtectConfig.values()) { if (protectConfig.isList()) {
			 * protect.getSettingsList().put(protectConfig,
			 * ess.getSettings().getProtectList(protectConfig.getConfigName())); } else if (protectConfig.isString()) {
			 * protect.getSettingsString().put(protectConfig,
			 * ess.getSettings().getProtectString(protectConfig.getConfigName())); } else {
			 * protect.getSettingsBoolean().put(protectConfig,
			 * ess.getSettings().getProtectBoolean(protectConfig.getConfigName(),
			 * protectConfig.getDefaultValueBoolean())); }
			 *
			 * }
			 */

			final ProtectHolder settings = protect.getSettings();
			settings.acquireReadLock();
			try
			{
				if (settings.getData().getDbtype().equalsIgnoreCase("mysql"))
				{
					try
					{
						protect.setStorage(new ProtectedBlockMySQL(
								settings.getData().getDburl(),
								settings.getData().getDbuser(),
								settings.getData().getDbpassword()));
					}
					catch (PropertyVetoException ex)
					{
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
				else
				{
					try
					{
						protect.setStorage(new ProtectedBlockSQLite("jdbc:sqlite:" + ess.getPlugin().getDataFolder() + "/EssentialsProtect.db"));
					}
					catch (PropertyVetoException ex)
					{
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
				/*if (protect.getSettingBool(ProtectConfig.memstore))
				 {
				 protect.setStorage(new ProtectedBlockMemory(protect.getStorage(), protect));
				 }*/

			}
			finally
			{
				settings.unlock();
			}
		}
	}
}
