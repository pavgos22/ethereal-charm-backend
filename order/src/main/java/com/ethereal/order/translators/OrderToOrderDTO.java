package com.ethereal.order.translators;

import com.ethereal.order.entity.*;
import org.springframework.stereotype.Component;

@Component
public class OrderToOrderDTO {

    public OrderDTO toOrderDTO(Order order) {
        if (order == null)
            return null;

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setUuid(order.getUuid());
        orderDTO.setOrders(order.getOrders());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setCustomerDetails(translateToCustomer(order));
        orderDTO.setAddress(translateAddress(order));
        orderDTO.setDeliver(translateDeliver(order.getDeliver()));
        orderDTO.setCompany(order.isCompany());
        orderDTO.setCompanyName(order.getCompanyName());
        orderDTO.setNip(order.getNip());
        orderDTO.setInfo(order.getInfo());
        return orderDTO;
    }

    private CustomerDetails translateToCustomer(Order order) {
        if (order == null)
            return null;

        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setFirstName(order.getFirstName());
        customerDetails.setLastName(order.getLastName());
        customerDetails.setEmail(order.getEmail());
        customerDetails.setPhone(order.getPhone());
        return customerDetails;
    }

    private Address translateAddress(Order order) {
        if (order == null)
            return null;

        Address address = new Address();
        address.setCity(order.getCity());
        address.setStreet(order.getStreet());
        address.setNumber(order.getNumber());
        address.setPostCode(order.getPostCode());
        return address;
    }

    private DeliverDTO translateDeliver(Deliver deliver) {
        if (deliver == null)
            return null;

        DeliverDTO deliverDTO = new DeliverDTO();
        deliverDTO.setUuid(deliver.getUuid());
        deliverDTO.setName(deliver.getName());
        deliverDTO.setPrice(deliver.getPrice());
        return deliverDTO;
    }
}
