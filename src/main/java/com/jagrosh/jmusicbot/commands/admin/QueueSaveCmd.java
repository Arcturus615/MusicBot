/*
 * Copyright 2020 Andrew Wyatt <sleepyfugu@gmail.com>.
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

import java.util.List;
import java.io.IOException;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.commands.AdminCommand;
import com.jagrosh.jmusicbot.audio.QueuedTrack;

/**
 *
 * @author Andrew Wyatt <sleepyfugu@gmail.com>
 */
public class QueueSaveCmd extends AdminCommand {
	private final Bot bot;
	public QueueSaveCmd(Bot bot)
	{
		this.bot = bot;
		this.name = "savequeue";
		this.help = "save the queue to a playlist";
		this.arguments = "<name>";
		this.aliases = bot.getConfig().getAliases(this.name);
	}

	@Override
	protected void execute(CommandEvent event)
	{
		if(!bot.getPlaylistLoader().folderExists())
			bot.getPlaylistLoader().createFolder();

		if(!bot.getPlaylistLoader().folderExists())
		{
			event.reply(event.getClient().getWarning()+" Playlists folder does not exist and could not be created!");
			return;
		}

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
				event.reply(event.getClient().getSuccess()+" Successfully created playlist `"+pname+"`!");
			}
			catch(IOException e)
			{
				event.reply(event.getClient().getError()+" I was unable to create the playlist: "+e.getLocalizedMessage());
				return;
			}
		}
		else
		{
			event.reply(event.getClient().getError()+" Playlist `"+pname+"` already exists!");
			return;
		}

		AudioHandler ah = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
		List<QueuedTrack> list = ah.getQueue().getList();

		if(list.isEmpty())
		{
			event.replyError("Cant save an empty playlist");
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		int total = 0;
		if(ah.isMusicPlaying(event.getJDA()))
		{
			builder.append("\r\n").append(ah.getPlayer().getPlayingTrack().getInfo().uri);
			total++;
		}
		ah.getQueue().getList().forEach(item -> builder.append("\r\n").append(item.getUrl()));
		total = total + ah.getQueue().getList().size();
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