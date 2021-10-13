## UGEBot
UGEBot is a private Discord bot made in Kotlin created for the different Discord servers of the Université Gustave Eiffel.
Its main purpose is to help manage the servers.

### Main features
- AutoRole : Automated system to allow any member to request a given role.
- Polls : Allow teachers to ask something to their students and gather the result in a file.
- Calls : Allow students to confirm their attendance during a class. A file, listing every present student, is sent to the teacher at the end.
- Temporary channels : Admins can link specific voice channels to a generator so that if someone joins it, the bot creates a temporary channel for them, that will disappear when it gets empty.
- Mathematical expression evaluation : Thanks to the Kotlin library [Keval](https://github.com/notKamui/Keval), anyone can quickly evaluate an expression.
- ASCII table generation : Anyone can quickly draw a table automatically formatted.
- Moodle Mails : Thanks to [Kourrier](https://github.com/notKamui/Kourrier), the bot can listen to the inbox of the university's mailbox, and dispatch the messages accordingly between the channels if they come from Moodle.

### Librairies used
- [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA)
- [LOGBack](http://logback.qos.ch/)
- [SLF4J](http://www.slf4j.org/)
- [Konfig](https://github.com/npryce/Konfig)
- [Keval](https://github.com/notKamui/Keval)
- [Kourrier](https://github.com/notKamui/Kourrier)

### Credits
- Irwin "Slama" Madet
- Jimmy "notKamui" Teillard
- ~~Lorris "ZwenDo" Creantor~~
