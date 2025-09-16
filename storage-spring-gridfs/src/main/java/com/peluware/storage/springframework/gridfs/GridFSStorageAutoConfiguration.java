package com.peluware.storage.springframework.gridfs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@Configuration
@ConditionalOnClass({GridFsTemplate.class, GridFsOperations.class})
public class GridFSStorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GridFSStorage gridFSStorage(GridFsTemplate gridFsTemplate, GridFsOperations gridFsOperations) {
        return new GridFSStorage(gridFsTemplate, gridFsOperations);
    }
}