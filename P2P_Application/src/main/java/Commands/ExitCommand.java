package main.java.Commands;

import main.java.Debian.NodeProperties;
import java.io.IOException;
import main.java.Debian.ChatClient;

import de.uniba.wiai.lspi.chord.service.ServiceException;
import main.java.Debian.PresenceService;

public class ExitCommand implements ChordCommand {
	private PresenceService _chordManager;
	
	public ExitCommand(PresenceService chordManager) {
		_chordManager = chordManager;
	}

	@Override
	public void execute() throws IOException, ServiceException {
            if (ChatClient._isMaster == false){
                _chordManager.unregister();
            }
            _chordManager.leaveNetwork();
            System.out.println("Node with Ip: " +NodeProperties.p2pIP+" leaves the network successfully.");
            System.exit(0);
	}
}
