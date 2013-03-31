package de.doridian.ccftp;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;

public class ComputerCraftFTP {
	public ComputerCraftFTP(int port, String bindAddress, String pasvPorts, String pasvAddress, String maxFilesize) throws FtpException {
		FtpServerFactory serverFactory = new FtpServerFactory();

		ListenerFactory factory = new ListenerFactory();

		//set the host and port of the listener
		if(bindAddress != null && !bindAddress.isEmpty() && bindAddress != ("0.0.0.0".trim()))
			factory.setServerAddress(bindAddress);

		factory.setPort(port);

        //Data connection config
        DataConnectionConfigurationFactory configurationFactory = new DataConnectionConfigurationFactory();
        if(pasvPorts != null && !pasvPorts.isEmpty()) {
            configurationFactory.setPassivePorts(pasvPorts);
            configurationFactory.setPassiveExternalAddress(pasvAddress);
        }
        factory.setDataConnectionConfiguration(configurationFactory.createDataConnectionConfiguration());

        //Connection config
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(false);
        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

		//replace the default listener
		serverFactory.addListener("default", factory.createListener());

		serverFactory.setUserManager(new CCUserManager(maxFilesize));

		//start the server
		FtpServer server = serverFactory.createServer();

		server.start();
	}
}
