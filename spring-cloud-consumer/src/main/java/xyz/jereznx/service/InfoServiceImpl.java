package xyz.jereznx.service;

import org.springframework.stereotype.Service;

/**
 * @author LQL
 * @since Create in 2020/8/9 15:48
 */
@Service
public class InfoServiceImpl implements InfoService {
    @Override
    public String info(String name) {
        return name + "fallbackOfFeign";
    }
}
