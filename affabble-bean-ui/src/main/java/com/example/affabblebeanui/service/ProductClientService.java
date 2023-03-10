package com.example.affabblebeanui.service;

import com.example.affabblebeanui.ds.CartBean;
import com.example.affabblebeanui.ds.ProductDto;
import com.example.affabblebeanui.dto.Product;
import com.example.affabblebeanui.dto.Products;
import com.example.affabblebeanui.exception.ProductNotFoundException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductClientService {

    private RestTemplate template=new RestTemplate();
    private List<Product> products;
    @PostConstruct
    public void init(){
        ResponseEntity<Products> response =template
                .getForEntity("http://localhost:8080/backend/products"
                        , Products.class);
        if (response.getStatusCode().is2xxSuccessful()){
            this.products=
                    response.getBody().getProducts();
            System.out.println(products);
        }
        else
            throw new RuntimeException("Error!");
    }
    public Product findProductById(int id){
        return products
                .stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(()->new
                        ProductNotFoundException(HttpStatus.NOT_FOUND
                        ,id +" NotFound!"));
    }
    public List<Product> findProductByCategoryName(String categoryName){
        return products.stream()
                .filter( p -> p.getCategoryName().equals(categoryName))
                .collect(Collectors.toList());
    }

    public List<Product> findAllProducts(){
        return this.products;
    }

    record TransferRequest(
            @JsonProperty("from_name") String fromName,
            @JsonProperty("from_email") String fromEmail,
            @JsonProperty("to_name") String toName,
            @JsonProperty("to_email") String toEmail,
            double amount
    ){

    }
    record TransPortInfoRequest(
            @JsonProperty("customer_name") String customerName,
            String email,
            Set<Product> products,
            @JsonProperty("total_amount") double totalAmount
    ){}
    public void checkout(String name, String email, double total) {
        var request=new TransferRequest(
                name,
                email,
                "william",
                "william@gmail.com",
                total
        );
        try {
            ResponseEntity<String> response=template
                    .postForEntity("http://localhost:8060/payment/transfer",request,String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully CheckOut...............");
                var transPortInfo = new TransPortInfoRequest(
                        name,
                        email,
                        cartBean.getCart(),
                        total
                );
                template.postForEntity("http://localhost:8050/transport/save-transport-info",transPortInfo, String.class);
            }
        }catch (HttpClientErrorException e) {
            String msg = String.format("%s and %s is not found in payment account!",email,name);
            throw new IllegalArgumentException(msg);
        }

    }

    @Autowired
    private CartBean cartBean;
}
