package com.ebsco.auth0.mgmt.samlbulkupdateauth0;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.ConnectionFilter;
import com.auth0.json.mgmt.Connection;
import com.auth0.net.Request;
import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@PropertySources({
		@PropertySource("classpath:auth0.properties")
})
public class SamlBulkUpdateAuth0Application {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Value(value = "${com.auth0.domain}")
	private String domain;

	@Value(value = "${com.auth0.mgmttoken}")
	private String mgmtToken;


    private RateLimiter rateLimiter = RateLimiter.create(1.8);

	public static void main(String[] args) {
		SpringApplication.run(SamlBulkUpdateAuth0Application.class, args);
	}

    @Bean
    public CommandLineRunner demo() {
        return (args) -> {

            ConnectionFilter connectionFilter = new ConnectionFilter();
            ManagementAPI mgmt = new ManagementAPI(domain, mgmtToken);
            List<Connection> connectionList = listAllExistingCloudDbConnections(mgmt,connectionFilter);
            deleteAllExistingCloudDbConnections(mgmt,connectionFilter);

            // when
            long startTime = System.currentTimeMillis() / 1000L;
            logger.info("Start time: "+startTime);

            for (int i = 0; i <500; i++){
                createANewConnection(mgmt, "automated-conn-"+i, connectionList.get(0).getOptions(), connectionList.get(0).getEnabledClients());
            }

            long elapsedTimeSeconds = (System.currentTimeMillis() / 1000L) - startTime;
            logger.info("Total time"+elapsedTimeSeconds);


        };
    }

    private List<Connection> listAllExistingCloudDbConnections(ManagementAPI mgmt, ConnectionFilter connectionFilter){
        // rate limit the initial call to get all connections
        rateLimiter.acquire(1);

	    Request<List<Connection>> request = mgmt.connections().list(connectionFilter);
        List<Connection> list = null;
        try {
            list = request.execute();
        }catch (Exception e){
            logger.error(e.getMessage());
        }

/*
        logger.info("======================================================");
        logger.info("name: "+list.get(0).getName());
        logger.info("id: "+list.get(0).getId());
        logger.info("provisiongTicketUrl: "+list.get(0).getProvisioningTicketUrl());
        logger.info("strategy: "+list.get(0).getStrategy());
        logger.info("+++++++++++++++++++ Enabled Clients +++++++++++++++++++");
        list.get(0).getEnabledClients().forEach(i->logger.info("Enabled Clients: "+i));
        logger.info("+++++++++++++++++++ Options +++++++++++++++++++");
        list.get(0).getOptions().forEach((k,v)->logger.info("Option: " + k + " Value : " + v));
*/

        return list;
    }


    private void deleteAllExistingCloudDbConnections(ManagementAPI mgmt, ConnectionFilter connectionFilter){
        logger.info("====================DELETING EXISTING CONNECTIONS====================");
        rateLimiter.acquire(1);
        Request<List<Connection>> request = mgmt.connections().list(connectionFilter.withStrategy("auth0"));
        List<Connection> list = null;
        try {
            list = request.execute();
        }catch (Exception e){
            logger.error(e.getMessage());
        }

        for (Connection connection: list) {
            deleteAnExistingCloudDbConnection(mgmt,connection.getId());
        }
        logger.info("====================DELETED ALL EXISTING AUTH0 U/PWD DB CONNECTIONS====================");
    }

    private void deleteAnExistingCloudDbConnection(ManagementAPI mgmt, String id){
        rateLimiter.acquire(1);
        Request request = mgmt.connections().delete(id);
        try {
            request.execute();
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }
    private void createANewConnection(ManagementAPI mgmt, String name, Map<String, Object> options, List<String> clients){
        rateLimiter.acquire(1);
        logger.info("$$$$$$$$$$$$$$$$$$ Creating a new connnection $$$$$$$$$$$$$$$$$$");
        Connection connection = new Connection(name, "auth0");
        connection.setOptions(options);
        connection.setEnabledClients(clients);
        Request<Connection> connectionCreateRequest = mgmt.connections().create(connection);

        try {
            connection = connectionCreateRequest.execute();
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

}