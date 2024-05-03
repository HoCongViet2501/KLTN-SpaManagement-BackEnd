package com.se.kltn.spamanagement.utils;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MappingData {
    public static ModelMapper modelMapper;

    @Autowired
    public MappingData(ModelMapper modelMapper) {
        MappingData.modelMapper = modelMapper;
    }


    public static <T, S> S mapObject(T source, Class<S> destination) {
        return modelMapper.map(source, destination);
    }

    public static <D, S> List<S> mapListObject(List<D> sources, Class<S> destination) {
        return sources.stream().map(source -> mapObject(source, destination)).collect(Collectors.toList());
    }

}
