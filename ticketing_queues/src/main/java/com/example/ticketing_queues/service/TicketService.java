package com.example.ticketing_queues.service;


import com.example.ticketing_queues.config.RabbitMqProperties;
import com.example.ticketing_queues.dto.TicketDto;
import com.example.ticketing_queues.entity.TicketCreation;
import com.example.ticketing_queues.producer.RabbitMqProducerService;
import com.example.ticketing_queues.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RabbitMqProducerService rabbitMqProducerService;

    @Autowired
    private RabbitMqProperties properties;

    @Autowired
    private MessageConverter messageConverter;

    @Autowired
    private AmqpTemplate amqpTemplate;
@Transactional
public TicketDto saveOrUpdateTicket(String id, TicketDto ticketDto) {
    // Check if required fields are null and handle them
    if (ticketDto.getTicketType() == null || ticketDto.getSiteName() == null) {
        throw new RuntimeException("TicketType and SiteName cannot be null.");
    }

    TicketCreation ticket;
    boolean isNew = id == null;

    if (isNew) {
        // Generate a new UUID for the ticket
        id = UUID.randomUUID().toString();
        ticket = new TicketCreation();
        ticket.setTicketId(id);
        ticket.setCreatedTime(LocalDateTime.now());
        if (ticketDto.getStatus() == null || ticketDto.getStatus().isEmpty()) {
            ticket.setStatus("new");
        }
    } else {
        // Find the existing ticket by ID
        ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        // Preserve the original created time
        ticket.setCreatedTime(ticket.getCreatedTime());
    }

    // Update ticket fields
    if (ticketDto.getTicketTitle() != null) {
        ticket.setTicketTitle(ticketDto.getTicketTitle());
    }
    if (ticketDto.getPriority() != null) {
        ticket.setPriority(ticketDto.getPriority());
    }
    if (ticketDto.getStatus() != null) {
        ticket.setStatus(ticketDto.getStatus());
    }
    if (ticketDto.getDescription() != null) {
        ticket.setDescription(ticketDto.getDescription());
    }

    // Set the ticketType and siteName
    ticket.setTicketType(ticketDto.getTicketType());
    ticket.setSiteName(ticketDto.getSiteName());

    // Determine the queue based on the updated title and status
    String queueName = determineQueue(ticket.getTicketTitle(), ticket.getStatus());
    ticket.setQueue(queueName);

    // Save the entity
    TicketCreation savedTicket = ticketRepository.save(ticket);

    // Convert entity back to DTO
    TicketDto savedTicketDto = modelMapper.map(savedTicket, TicketDto.class);

    // Send the ticket to RabbitMQ
    new Thread(() -> {
        try {
            String routingKey = determineRoutingKey(queueName);
            rabbitMqProducerService.sendMessageToQueue(savedTicket, queueName, routingKey);
        } catch (Exception e) {
            e.printStackTrace(); // Log the error or handle it appropriately
        }
    }).start();

    return savedTicketDto;
}


    public TicketDto getTicketById(String id) {
        // Find the ticket by ID
        TicketCreation ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Convert entity to DTO and return
        return modelMapper.map(ticket, TicketDto.class);
    }


    public void deleteTicket(String id) {
        // Find the ticket by ID
        TicketCreation ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Delete the ticket
        ticketRepository.delete(ticket);
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
                case "Open" -> "FIELDMANAGER";
                default -> "DEFAULT";
            };
            case "Camera Position Changed" -> switch (status) {
                case "Closed" -> "FIELDREPRESENTATIVE";
                case "Fixed", "New" -> "FIELDMANAGER";
                case "Open" -> "HELPDESK";
                default -> "DEFAULT";
            };
            case "Client Request" -> switch (status) {
                case "Closed", "Open" -> "HELPDESK";
                case "New" -> "FIELDMANAGER";
                default -> "DEFAULT";
            };
            case "Hard Disk Problem" -> switch (status) {
                case "Closed" -> "FIELDREPRESENTATIVE";
                case "New" -> "HELPDESK";
                default -> "DEFAULT";
            };
            case "HDD Not Recording" -> {
                if (status.equals("Closed")) {
                    yield "HELPDESK";
                }
                yield "DEFAULT";
            }
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
