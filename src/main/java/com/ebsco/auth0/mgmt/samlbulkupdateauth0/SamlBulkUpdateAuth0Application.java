package com.ebsco.auth0.mgmt.samlbulkupdateauth0;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.ConnectionFilter;
import com.auth0.json.mgmt.Connection;
import com.auth0.net.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.util.List;

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

	public static void main(String[] args) {
		SpringApplication.run(SamlBulkUpdateAuth0Application.class, args);
	}

    @Bean
    public CommandLineRunner demo() {
        return (args) -> {

            ConnectionFilter connectionFilter = new ConnectionFilter();

            ManagementAPI mgmt = new ManagementAPI(domain, mgmtToken);

            Request<List<Connection>> request = mgmt.connections().list(connectionFilter);
            List<Connection> list = null;
            try {
                list = request.execute();
            }catch (Exception e){
                logger.error(e.getMessage());
            }

            logger.info("======================================================");
            logger.info("name: "+list.get(0).getName());
            logger.info("id: "+list.get(0).getId());
            logger.info("provisiongTicketUrl: "+list.get(0).getProvisioningTicketUrl());
            logger.info("strategy: "+list.get(0).getStrategy());
            logger.info("+++++++++++++++++++ Enabled Clients +++++++++++++++++++");
            list.get(0).getEnabledClients().forEach(i->logger.info("Enabled Clients: "+i));
            logger.info("+++++++++++++++++++ Options +++++++++++++++++++");
            list.get(0).getOptions().forEach((k,v)->logger.info("Option: " + k + " Value : " + v));

            logger.info("$$$$$$$$$$$$$$$$$$ Creating a new connnection $$$$$$$$$$$$$$$$$$");
            Connection connection = new Connection("automated-conn-0", "auth0");
            connection.setOptions(list.get(0).getOptions());
            connection.setEnabledClients(list.get(0).getEnabledClients());
            Request<Connection> connectionCreateRequest = mgmt.connections().create(connection);

            try {
                connection = connectionCreateRequest.execute();
            }catch (Exception e){
                logger.error(e.getMessage());
            }

        };
    }
}