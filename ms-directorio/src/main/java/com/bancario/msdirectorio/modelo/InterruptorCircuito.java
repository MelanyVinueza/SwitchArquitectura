package com.bancario.msdirectorio.modelo;

import java.time.LocalDateTime;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.bancario.msdirectorio.converter.LocalDateTimeConverter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DynamoDBDocument
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterruptorCircuito {

    @DynamoDBAttribute(attributeName = "estaAbierto")
    private boolean estaAbierto;

    @DynamoDBAttribute(attributeName = "fallosConsecutivos")
    private int fallosConsecutivos;

    @DynamoDBTypeConverted(converter = LocalDateTimeConverter.class)
    @DynamoDBAttribute(attributeName = "ultimoFallo")
    private LocalDateTime ultimoFallo;
}
