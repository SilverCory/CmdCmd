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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Created by Cory Redmond on 31/01/2016.
 *
 * @author Cory Redmond <ace@ac3-servers.eu>
 */
public class CmdCmdPlugin extends JavaPlugin {

    public static String perm_message = "&cYou do not have permission to use this command!";
    private File commandsDir;

    @Override
    public void onLoad() {

        ConfigurationSerialization.registerClass( AliasCommand.class );

        if( !new File( getDataFolder(), "config.yml" ).exists() )
            saveDefaultConfig();

        Logs.get( CmdCmdPlugin.class, getLogger(), getConfig().getBoolean("debug", false) )._D("Debugging is enabled!");
        perm_message = getConfig().getString( "perm-message", "&cYou do not have permission to use this command!" );

        if(!( this.commandsDir = new File( getDataFolder(), "commands" ) ).exists()) {
            this.commandsDir.mkdirs();
            try {
                File exampleFile = new File( this.commandsDir, "example_command.yml" );
                Files.copy(getResource("example_command.yml"), exampleFile.toPath());

                YamlConfiguration config = YamlConfiguration.loadConfiguration(exampleFile);

                ArrayList<String> aliases = new ArrayList<>();
                aliases.add("exam");
                aliases.add("exampl");
                ArrayList<String> consoleCommands = new ArrayList<>();
                consoleCommands.add( "msg %player% Hey, thanks! We'll give you 5 stone." );
                consoleCommands.add( "give %player% stone 5" );
                ArrayList<String> playerCommands = new ArrayList<>();
                playerCommands.add( "me just got 5 stone from doing /example!" );
                playerCommands.add( "you_can add more commands." );

                config.set("a", new AliasCommand("example", "An example command", "example.command", "&c/example", aliases, consoleCommands, playerCommands));

                config.save(exampleFile);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onEnable() {

        for( File command : getYamlFiles( commandsDir ) ) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(command);
            for( String key : config.getKeys(false) )
                ((AliasCommand)config.get( key )).load();
        }

    }

    @Override
    public void onDisable() {
        try {
            AliasCommand.Injector.unloadAliases();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException( "Unable to unload commands.\nInjection into the command map failed!", e );
        }
    }

    public static File[] getYamlFiles(File dirFile) {
        return dirFile.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.toLowerCase().endsWith(".yml") || filename.toLowerCase().endsWith(".yaml");
            }
        });
    }

}
