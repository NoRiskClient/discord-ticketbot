# How to Run the Discord Ticket Bot

## Prerequisites
- **Java 21 or higher** installed
- A **Discord Bot Token** (from Discord Developer Portal)
- Your **Discord Server (Guild) ID**

---

## Method 1: Run Locally with Gradle (Recommended for Development)

### Step 1: Build the Project
Open PowerShell in the project directory and run:
```powershell
.\gradlew.bat shadowJar
```

This will compile the code and create a JAR file at:
`build\libs\discord-ticketbot.jar`

### Step 2: Create Configuration Directory
```powershell
mkdir Tickets
```

### Step 3: Create Configuration File
Copy the example config and edit it:
```powershell
Copy-Item config.example.yml Tickets\config.yml
```

Now open `Tickets\config.yml` in a text editor and **at minimum** set:
- `token: "YOUR_BOT_TOKEN_HERE"` - Replace with your Discord bot token

Optional settings you may want to configure:
- `staffId` - Role ID for staff members who can manage tickets
- `logChannel` - Channel ID for ticket transcripts
- `maxTicketsPerUser` - Maximum open tickets per user (default: 3)
- Other settings as needed

### Step 4: Run the Bot
```powershell
java -jar build\libs\discord-ticketbot.jar
```

### Step 5: Set Up in Discord
1. Invite your bot to your Discord server (needs permissions: Manage Channels, Manage Threads, Send Messages, Embed Links, Attach Files, Manage Roles)
2. In Discord, run the command: `/ticket setup`
3. This will automatically configure your `serverId` and create necessary channels/categories

---

## Method 2: Run with Docker

### Using Docker Compose (Easiest)
```powershell
docker-compose up -d
```

Before running, make sure to:
1. Create the `Tickets` directory: `mkdir Tickets`
2. Copy and edit the config: `Copy-Item config.example.yml Tickets\config.yml`
3. Set your bot token in `Tickets\config.yml`

### Using Docker Directly
```powershell
# Build the image
docker build -t discord-ticketbot .

# Run the container
docker run -d --name ticketbot -v ${PWD}/Tickets:/app/Tickets discord-ticketbot
```

---

## Getting Your Bot Token

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application or select an existing one
3. Go to the "Bot" section
4. Click "Reset Token" to get your bot token (keep it secret!)
5. Enable these Privileged Gateway Intents:
    - SERVER MEMBERS INTENT
    - MESSAGE CONTENT INTENT
6. Go to OAuth2 > URL Generator:
    - Select scope: `bot`, `applications.commands`
    - Select permissions: Administrator (or specific permissions as needed)
    - Copy the generated URL and use it to invite the bot to your server

---

## Configuration Overview

Key settings in `Tickets/config.yml`:

| Setting | Required | Description |
|---------|----------|-------------|
| `token` | ✓ | Your Discord bot token |
| `serverId` | Auto | Set automatically by `/ticket setup` |
| `staffId` | - | Staff role ID for ticket management |
| `logChannel` | - | Where transcripts are posted |
| `ratingStatsChannel` | - | Daily rating statistics |
| `pendingRatingCategory` | - | Category for tickets awaiting rating |
| `maxTicketsPerUser` | - | Max open tickets per user (default: 3) |
| `xpApiUrl` / `xpApiKey` | - | Optional XP system integration |

---

## Troubleshooting

**"Invalid Token" error**:
- Check that your bot token is correct in `Tickets/config.yml`
- Make sure there are no extra spaces or quotes

**"Missing Permissions" error**:
- Ensure your bot has Administrator permission or at least:
    - Manage Channels, Manage Threads, Send Messages, Embed Links, Attach Files, Manage Roles

**"Database error"**:
- The bot creates a SQLite database automatically in the `Tickets` folder
- Make sure the bot has write permissions to this directory

**Commands not showing**:
- Make sure you've enabled `applications.commands` scope when inviting the bot
- Wait a few minutes for Discord to sync commands
- Try running `/ticket setup` to register commands

---

## Default Ticket Categories

The bot supports these ticket types:
- General Support
- Bug Report
- Crash Report
- Payment Issue
- Report User
- Security Issue

You can customize categories by modifying the category classes in `src/main/java/eu/greev/dcbot/ticketsystem/categories/`

---

## Next Steps After Running

1. Run `/ticket setup` in your Discord server
2. Configure staff roles with `staffId` in config
3. Set up log channels for transcripts
4. Test creating a ticket
5. Explore other commands like `/stats`, `/cleanup`, etc.

Enjoy your new ticket system! 🎫
