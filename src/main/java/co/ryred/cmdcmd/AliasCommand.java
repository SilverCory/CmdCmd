/*
 * http://ryred.co/
 * ace[at]ac3-servers.eu
 *
 * =================================================================
 *
 * Copyright (c) 2016, Cory Redmond
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of CmdCmd nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package co.ryred.cmdcmd;

import co.ryred.red_commons.Logs;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Cory Redmond on 31/01/2016.
 *
 * @author Cory Redmond <ace@ac3-servers.eu>
 */
public class AliasCommand extends Command implements ConfigurationSerializable {

    @Getter
    private String permission = null;

    @Getter
    private List<String> consoleCommands;

    @Getter
    private List<String> playerCommands;

    public AliasCommand(String name, String description, String permission, String usageMessage, List<String> aliases, List<String> consoleCommands, List<String> playerCommands) {
        super(name, description, usageMessage, aliases);
        this.permission = permission;
        this.consoleCommands = consoleCommands;
        this.playerCommands = playerCommands;

    }

    @Override
    public Map<String, Object> serialize() {

        HashMap<String, Object> ret = new HashMap<>();

        ret.put( "name", getName() );
        ret.put( "description", getDescription() );
        ret.put( "permission", getPermission() );
        ret.put( "usage", getUsage() );
        ret.put( "aliases", getAliases() );
        ret.put( "console-commands", getConsoleCommands() );
        ret.put( "player-commands", getPlayerCommands() );

        return ret;

    }

    public static AliasCommand deserialize( Map<String, Object> map ) {

        String name = (String)map.get("name");
        String description = (String)map.get("description");
        String permission = (String)map.get("permission");
        String usage = (String)map.get("usage");
        List<String> aliases = (List<String>)map.get("aliases");
        List<String> consoleCommands = (List<String>)map.get("console-commands");
        List<String> playerCommands = (List<String>)map.get("player-commands");

        if(usage != null) usage = ChatColor.translateAlternateColorCodes('&', usage);

        if( (consoleCommands == null || consoleCommands.isEmpty()) && (playerCommands == null || playerCommands.isEmpty()) )
            Logs.get(CmdCmdPlugin.class).warning("Alias command \"" + name + "\" has no commands to run!");

        return new AliasCommand( name, description, permission, usage, aliases, consoleCommands, playerCommands );

    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        try {
            doCommand( sender, label, args );
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void doCommand(CommandSender sender, String label, String[] args) {

        if( permission == null || !sender.hasPermission( permission ) ) {
            sender.sendMessage( ChatColor.translateAlternateColorCodes('&', CmdCmdPlugin.perm_message) );
            return;
        }

        for( String cmd : consoleCommands )
            Bukkit.getServer().dispatchCommand( Bukkit.getConsoleSender(), cmd.replace("%player%", sender.getName()) );

        for( String cmd : playerCommands )
            Bukkit.getServer().dispatchCommand( sender, cmd.replace("%player%", sender.getName()) );

    }

    public void load() {
        try {
            Injector.getCommandMap().register( "CmdCmd", this );
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException( "Unable to load command \"" + getName() + "\".\nInjection into the command map failed!", e );
        }
    }

    public static class Injector {

        private static Field commandMapField;
        private static Field mapField;

        public static Field getCommandMapField() throws NoSuchFieldException {
            return commandMapField == null ? commandMapField = SimplePluginManager.class.getDeclaredField("commandMap") : commandMapField;
        }

        public static Field getMapField() throws NoSuchFieldException {
            return mapField == null ? mapField = SimpleCommandMap.class.getDeclaredField("knownCommands") : mapField;
        }

        public static SimpleCommandMap getCommandMap() throws IllegalAccessException, NoSuchFieldException {
            getCommandMapField().setAccessible(true);
            return (SimpleCommandMap) getCommandMapField().get(Bukkit.getPluginManager());
        }

        public static void unloadAliases() throws IllegalAccessException, NoSuchFieldException {
            getMapField().setAccessible(true);
            Map<String, Command> map = (Map<String, Command>)getMapField().get( getCommandMap() );

            Iterator<Map.Entry<String, Command>> entryIteratpr = map.entrySet().iterator();
            while( entryIteratpr.hasNext() ) {
                Map.Entry<String, Command> entry = entryIteratpr.next();
                if( entry.getValue() instanceof AliasCommand )
                    entryIteratpr.remove();
            }

        }

    }

}
