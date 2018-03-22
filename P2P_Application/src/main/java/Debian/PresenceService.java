package main.java.Debian;

import de.uniba.wiai.lspi.chord.service.ServiceException;

public interface PresenceService {
    boolean register() throws ServiceException;
    void unregister() throws ServiceException;
    void leaveNetwork();
    String lookup(String name) throws ServiceException;
}