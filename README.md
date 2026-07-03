# GuildPlugin

Guild (csapat) rendszer PurPur 1.18.2 (Java 17) szerverekhez.

## Fontos megjegyzés

A leírásodban nem szerepelt guild **létrehozó** parancs, ezért hozzáadtam:

```
/guild create <nev>   - Guild letrehozasa, a letrehozo lesz a Leader
```

Emellett most bekerült a `/guild kick <jatekos>` parancs is (Co-Leader/Leader
használhatja): a Leader bárkit kirúghat, a Co-Leader csak sima tagot (másik
Co-Leadert vagy a Leadert nem), saját magát pedig senki - arra a `/guild
leave` való.

Enélkül a rendszer nem lenne használható (nem lenne mit meghívni/elfogadni).
Ha máshogy szeretnéd (pl. csak admin hozhat létre guildet, vagy shoppal
fizetni kelljen érte), szólj és átírom.

## Build

A projekt Maven-t használ. Mivel a build lépés letölti a Spigot API-t és a
PlaceholderAPI-t a Maven repository-kból, ehhez internet-kapcsolat kell a
gépeden:

```bash
mvn clean package
```

A kész jar itt lesz: `target/GuildPlugin-1.0.0.jar`

Ha nincs helyi Spigot API a Maven cache-edben, előbb buildeld le a
BuildTools-szal 1.18.2-re (`java -jar BuildTools.jar --rev 1.18.2`), vagy
használj egy publikus Spigot/Paper API mirrort (pl. PaperMC repo is
kompatibilis API szinten).

## Telepítés

1. Másold a `GuildPlugin-1.0.0.jar` fájlt a szerver `plugins` mappájába.
2. Opcionális: telepítsd a **PlaceholderAPI**-t, ha a placeholdereket
   használni szeretnéd chatben/scoreboardon/tabliston stb.
3. Indítsd újra a szervert. A `config.yml` a `plugins/GuildPlugin/`
   mappában jön létre.

## Parancsok

Lásd a `plugin.yml`-t és a `/guild` súgót (`/guild` parancs önmagában).

Admin parancsokhoz a `guild.admin` jogosultság szükséges (alapból OP-nak jár).

## Placeholderek (PlaceholderAPI)

| Placeholder | Leírás |
|---|---|
| `%guild_name%` | `&8[&r<szín><név>&8]&r`, vagy üres string ha nincs guild |
| `%guild_members_count%` | pl. `3/10` |
| `%guild_rating%` | helyezés a killek alapján rendezett listában |
| `%guild_kd%` | K/D arány (2 tizedesjegy) |
| `%guild_kills%` | killek száma |
| `%guild_deaths%` | halálok száma |
| `%guild_top_name_<n>%` | n. helyezett guild neve |
| `%guild_top_kills_<n>%` | n. helyezett guild killjei |
| `%guild_top_formatted_<n>%` | `&6NÉV &7- &6KILLEK` |

## Killek/halálok követése

A plugin automatikusan növeli a guild kill/death számlálóját
`PlayerDeathEvent` alapján: ha egy guild-tag megöl egy másik játékost,
a gyilkos guildje +1 killt kap, az áldozat guildje +1 halált.
