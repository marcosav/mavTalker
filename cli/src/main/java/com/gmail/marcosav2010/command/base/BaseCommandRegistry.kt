package com.gmail.marcosav2010.command.base

import com.gmail.marcosav2010.command.CommandRegistry

class BaseCommandRegistry : CommandRegistry(setOf(ExitCMD(), VerboseCMD(), InfoCMD(), StopCMD(), NewCMD(), HelpCMD(),
        DisconnectCMD(), GenerateAddressCMD(), ConnectionKeyCMD(), PeerPropertyCMD(),
        PingCMD())) 