package com.ecommerce.inventory.dto.map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.inventory.dto.CreateProductRequest;
import com.ecommerce.inventory.dto.ProductResponse;
import com.ecommerce.inventory.model.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "sku", source = "product.sku")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "description", source = "product.description")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "totalStock", source = "product.totalStock")
    @Mapping(target = "availableStock", source = "availableStock")
    ProductResponse toResponse(Product product, Integer availableStock);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalStock", source = "stockQuantity")
    @Mapping(target = "availableStock", source = "stockQuantity")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(CreateProductRequest request);
}
