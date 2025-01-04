package com.ethereal.order.service;

import com.ethereal.order.entity.DeliverDTO;
import com.ethereal.order.repository.DeliverRepository;
import com.ethereal.order.translators.DeliverToDeliverDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliverService {

    private final DeliverRepository deliverRepository;
    private final DeliverToDeliverDTO deliverToDeliverDTO;

    public List<DeliverDTO> getAllDeliver() {
        return deliverRepository.findAll().stream().map(deliverToDeliverDTO::deliverDTO).collect(Collectors.toList());
    }
}
