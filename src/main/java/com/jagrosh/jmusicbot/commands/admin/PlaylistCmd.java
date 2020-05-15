/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.admin;

import java.io.IOException;
import java.util.List;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.AdminCommand;
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class PlaylistCmd extends AdminCommand 
{
	private final Bot bot;

    public PlaylistCmd(Bot bot)
    {
		this.bot = bot;
        this.guildOnly = false;
        this.name = "playlist";
        this.arguments = "<append|delete|make|setdefault>";
        this.help = "playlist management";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.children = new AdminCommand[]{
            new ListCmd(),
            new AppendlistCmd(),
            new DeletelistCmd(),
            new MakelistCmd(),
            new DefaultlistCmd(bot)
		};
    }

	private void SetupDirectory(CommandEvent event)
	{
		if(!bot.getPlaylistLoader().folderExists())
			bot.getPlaylistLoader().createFolder();
		if(!bot.getPlaylistLoader().folderExists())
		{
			event.reply(event.getClient().getWarning()+" Playlists folder does not exist and could not be created!");
			return;
		}
	}

    @Override
    public void execute(CommandEvent event) 
    {
		// Make sure our directory even exists before we do anything.
		SetupDirectory(event);

		StringBuilder builder = new StringBuilder(event.getClient().getWarning()
			+" Playlist Management Commands:\n");
        for(Command cmd: this.children)
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(" ").append(cmd.getArguments()==null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
        event.reply(builder.toString());
    }
    
    public class MakelistCmd extends AdminCommand 
    {
        public MakelistCmd()
        {
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = "makes a new playlist";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if(bot.getPlaylistLoader().getPlaylist(pname)==null)
            {
                try
                {
                    bot.getPlaylistLoader().createPlaylist(pname);
                    event.reply(event.getClient().getSuccess()+" Successfully created playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to create the playlist: "+e.getLocalizedMessage());
                }
            }
            else
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` already exists!");
        }
    }
    
    public class DeletelistCmd extends AdminCommand 
    {
        public DeletelistCmd()
        {
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "deletes an existing playlist";
            this.arguments = "<name>";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            String pname = event.getArgs().replaceAll("\\s+", "_");
            if(bot.getPlaylistLoader().getPlaylist(pname)==null)
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` doesn't exist!");
            else
            {
                try
                {
                    bot.getPlaylistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess()+" Successfully deleted playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to delete the playlist: "+e.getLocalizedMessage());
                }
            }
        }
    }
    
    public class AppendlistCmd extends AdminCommand 
    {
        public AppendlistCmd()
        {
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = "appends songs to an existing playlist";
            this.arguments = "<name> <URL> | <URL> | ...";
            this.guildOnly = false;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            String[] parts = event.getArgs().split("\\s+", 2);
            if(parts.length<2)
            {
                event.reply(event.getClient().getError()+" Please include a playlist name and URLs to add!");
                return;
            }
            String pname = parts[0];
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(pname);
            if(playlist==null)
                event.reply(event.getClient().getError()+" Playlist `"+pname+"` doesn't exist!");
            else
            {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String[] urls = parts[1].split("\\|");
                for(String url: urls)
                {
                    String u = url.trim();
                    if(u.startsWith("<") && u.endsWith(">"))
                        u = u.substring(1, u.length()-1);
                    builder.append("\r\n").append(u);
                }
                try
                {
                    bot.getPlaylistLoader().writePlaylist(pname, builder.toString());
                    event.reply(event.getClient().getSuccess()+" Successfully added "+urls.length+" items to playlist `"+pname+"`!");
                }
                catch(IOException e)
                {
                    event.reply(event.getClient().getError()+" I was unable to append to the playlist: "+e.getLocalizedMessage());
                }
            }
        }
    }
    
    public class DefaultlistCmd extends AutoplaylistCmd 
    {
        public DefaultlistCmd(Bot bot)
        {
            super(bot);
            this.name = "setdefault";
            this.aliases = new String[]{"default"};
            this.arguments = "<playlistname|NONE>";
            this.guildOnly = true;
        }
    }
    
    public class ListCmd extends AdminCommand 
    {
        public ListCmd()
        {
            this.name = "all";
            this.aliases = new String[]{"available","list"};
            this.help = "lists all available playlists";
            this.guildOnly = true;
        }

        @Override
        protected void execute(CommandEvent event) 
        {
            List<String> list = bot.getPlaylistLoader().getPlaylistNames();
            if(list==null)
                event.reply(event.getClient().getError()+" Failed to load available playlists!");
            else if(list.isEmpty())
                event.reply(event.getClient().getWarning()+" There are no playlists in the Playlists folder!");
            else
            {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess()+" Available playlists:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }
	}
	
	public class SaveQueueCmd extends AdminCommand
	{
		public SaveQueueCmd()
		{
			this.name = "save";
			this.help = "save the queue to a playlist";
			this.arguments = "<name>";
			this.guildOnly = true;
		}

		@Override
		protected void execute(CommandEvent event)
		{
			if (event.getArgs().isEmpty())
			{
				event.replyError("Please provide a name for the new playlist");
				return;
			}
			
			String pname = event.getArgs().replaceAll("\\s+", "_");
			
			if(bot.getPlaylistLoader().getPlaylist(pname)==null)
			{
				try
				{
					bot.getPlaylistLoader().createPlaylist(pname);
				}
				catch(IOException e)
				{
					event.reply(event.getClient().getError()+" Failed to create the playlist: "+e.getLocalizedMessage());
					return;
				}
			}
			else
			{
				event.reply(event.getClient().getError()+" Playlist `"+pname+"` already exists!");
				return;
			}

			AudioHandler ah = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
			List<QueuedTrack> alist = ah.getQueue().getList();
			StringBuilder builder = new StringBuilder();
			int total = 0;

			if(alist.isEmpty())
			{
				event.replyError(" Cant save an empty playlist");
				return;
			}

			if(ah.isMusicPlaying(event.getJDA()))
			{
				// Save the current track as well.
				builder.append("\r\n").append(ah.getPlayer().getPlayingTrack().getInfo().uri);
				total++;
			}

			alist.forEach(item -> builder.append("\r\n").append(item.getUrl()));
			total = total + alist.size();
			try{
				bot.getPlaylistLoader().writePlaylist(pname, builder.toString());
				event.reply(event.getClient().getSuccess()+" Successfully saved "+total+" items as playlist `"+pname+"`!");
			}
			catch(IOException e)
			{
				event.reply(event.getClient().getError()+" Unable to save the playlist: "+e.getLocalizedMessage());
			}
		}
	}

	public class RemoveCmd extends AdminCommand
	{
		public RemoveCmd()
		{
			this.name = "remove";
			this.help = "removes specified URLs from a playlist";
			this.arguments = "<name> | <URL> | <URL> | ...";
			this.guildOnly = true;
		}

		@Override
		protected void execute(CommandEvent event)
		{
			int hits = 0;

			String[] parts = event.getArgs().split("\\s+", 2);
			if(parts.length<2)
			{
				event.reply(event.getClient().getError()+" Please include a playlist name and URLs to remove!")
				return;
			}

			String pname = parts[0];
			Playlist playlist = bot.getPlaylistLoader().getPlaylist(pname);
			StringBuilder builder = new StringBuilder();

			if(playlist==null)
			{
				event.reply(event.getClient().getError()+" Playlist `"+pname+"` doesn't exist!");
				return;
			}

			String[] urls = parts[1].split("\\|");

			playlist.getItems().forEach(item -> {
					for (String url : urls)
					{
						String u = url.trim();
						if(u.startsWith("<") && u.endsWith(">"))
							u = u.substring(1, u.length()-1);
						if (!url.equals(item))
						{
							builder.append("\r\n").append(item);
						}
						else
						{
							hits++;
						}
					}
			});

			if (hits>0)
			{
				event.replyError("No URL matches found");
				return;
			}

			try
			{
				bot.getPlaylistLoader().writePlaylist(pname, builder.toString());
				event.reply(event.getClient().getSuccess()+" Successfully removed "+hits+" items from playlist `"+pname+"`!");
			}
			catch(IOException e)
			{
				event.reply(event.getClient().getError()+" I was unable to save to the playlist: "+e.getLocalizedMessage());
			}
		}
	}
}
