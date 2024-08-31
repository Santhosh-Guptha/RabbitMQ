package com.example.ticketing_queues.controller;



import com.example.ticketing_queues.dto.TicketDto;
import com.example.ticketing_queues.service.RabbitMqConsumerService;
import com.example.ticketing_queues.config.RabbitMqProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/consume")
public class RabbitMqConsumerController {

    @Autowired
    private RabbitMqConsumerService rabbitMqConsumerService;

    @Autowired
    private RabbitMqProperties properties;

    @GetMapping("/{title}/{status}")
    public String consumeMessageFromQueue(@PathVariable String title, @PathVariable String status) {
        String queueName = determineQueue(title, status);
        String routingKey = determineRoutingKey(queueName);

        System.out.println(routingKey+"routingKey");

        // Use the routing key to retrieve the message from the corresponding queue
        String message = rabbitMqConsumerService.consumeMessage(routingKey);

        if (message != null) {
            return "Message consumed from " + queueName + " queue and processed successfully.";
        } else {
            return "No messages available in the " + queueName + " queue.";
        }
    }

    private String determineQueue(String title, String status) {
        return switch (title) {
            case "Site Installation" -> switch (status) {
                case "new", "closed" -> "SITEMANAGER";
                case "open", "fixed" -> "CRM";
                default -> "DEFAULT";
            };
            case "preventive maintanance", "site down", "site relocation", "site renovation" -> switch (status) {
                case "new", "open", "fixed", "closed" -> "FIELD MANAGER";
                default -> "DEFAULT";
            };
            case "two way audio not working/audio issue" -> switch (status) {
                case "new", "open", "closed" -> "FIELD MANAGER";
                case "fixed" -> "FIELD MANAGER/HELPDESK";
                default -> "DEFAULT";
            };
            case "Camera Disconnected" -> switch (status) {
                case "Closed" -> "FIELDREPRESENTATIVE";
                case "New" -> "HELPDESK";
                case "Open" -> "FIELD MANAGER";
                default -> "DEFAULT";
            };
            case "Camera Position Changed" -> switch (status) {
                case "Closed" -> "FIELDREPRESENTATIVE";
                case "Fixed", "New" -> "FIELD MANAGER";
                case "Open" -> "HELPDESK";
                default -> "DEFAULT";
            };
            case "Client Request" -> switch (status) {
                case "Closed", "Open" -> "HELPDESK";
                case "New" -> "FIELD MANAGER";
                default -> "DEFAULT";
            };
            case "Hard Disk Problem" -> switch (status) {
                case "Closed" -> "FIELDREPRESENTATIVE";
                case "New" -> "HELPDESK";
                default -> "DEFAULT";
            };
            case "HDD Not Recording" -> status.equals("Closed") ? "HELPDESK" : "DEFAULT";
            default -> "DEFAULT";
        };
    }

    private String determineRoutingKey(String queueName) {
        return switch (queueName) {
            case "CRM" -> properties.getRoutingKey().getCrm();
            case "HELPDESK" -> properties.getRoutingKey().getHelpdesk();
            case "FIELD MANAGER" -> properties.getRoutingKey().getFieldmanager();
            case "FIELDREPRESENTATIVE" -> properties.getRoutingKey().getFieldrepresentative();
            case "TICKETANALYSIS" -> properties.getRoutingKey().getTicketanalysis();
            case "SITEMANAGER" -> properties.getRoutingKey().getSitemanager();
            default -> properties.getRoutingKey().getDefaultRoutingKey();
        };
    }
}
