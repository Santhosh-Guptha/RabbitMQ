package com.example.ticketing_queues.controller;

import com.example.ticketing_queues.dto.TicketDto;
import com.example.ticketing_queues.entity.TicketCreation;
import com.example.ticketing_queues.service.RabbitMqConsumerService;
import com.example.ticketing_queues.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketDto> createTicket(@RequestBody TicketDto ticketDto) {
        // Use a dummy ID (or a specific logic) to indicate creation
        TicketDto createdTicket = ticketService.saveOrUpdateTicket(null, ticketDto);
        return new ResponseEntity<>(createdTicket, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketDto> updateTicket(@PathVariable String id, @RequestBody TicketDto ticketDto) {
        // Update the ticket using the provided ID
        TicketDto updatedTicket = ticketService.saveOrUpdateTicket(id, ticketDto);
        return new ResponseEntity<>(updatedTicket, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getTicket(@PathVariable String id) {
        TicketDto ticket = ticketService.getTicketById(id);
        return new ResponseEntity<>(ticket, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable String id) {
        ticketService.deleteTicket(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    @Autowired
    private RabbitMqConsumerService rabbitMqConsumerService;

    @GetMapping("/{queueName}/consume")
    public ResponseEntity<List<TicketCreation>> consumeMessages(
            @PathVariable String queueName,
            @RequestParam(defaultValue = "1") int count) {
        List<TicketCreation> tickets = rabbitMqConsumerService.consumeMessagesFromQueue(queueName, count);
        return ResponseEntity.ok(tickets);
    }
}
