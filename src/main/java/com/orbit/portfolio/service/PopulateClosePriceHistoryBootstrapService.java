package com.orbit.portfolio.service;

import com.orbit.portfolio.model.ClosePriceHistory;
import com.orbit.portfolio.model.enums.AssetType;
import com.orbit.portfolio.repository.ClosePriceHistoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PopulateClosePriceHistoryBootstrapService {
    private final ClosePriceHistoryRepository closePriceHistoryRepository;
    
    public PopulateClosePriceHistoryBootstrapService(ClosePriceHistoryRepository closePriceHistoryRepository) {
        this.closePriceHistoryRepository = closePriceHistoryRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void populateClosePriceHistory(){
        if (closePriceHistoryRepository.count() == 0) {
            //print empty table
            System.out.println("ClosePriceHistory table is empty.");
        } else if (closePriceHistoryRepository.count() > 0) {
            List<ClosePriceHistory> closePriceHistoryList = closePriceHistoryRepository.findAll();
            //for loop each row check in closePriceHistory table and look for closePrice==null or closePriceTimestamp==null or closePriceTimestamp==yesterday date, if found then set that closePrice
            for(int i=0; i<closePriceHistoryList.size(); i++){
                ClosePriceHistory cph = closePriceHistoryList.get(i);
                if(cph.getClosePrice() == null || cph.getClosePriceTimestamp() == null || cph.getClosePriceTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalDate().isBefore(java.time.LocalDate.now())){
                    //fetch close price using priceFetcher and set it in closePriceHistory table
                    String assetName = cph.getAsset().getAssetName();
                    AssetType assetType = cph.getAsset().getAssetType();

                    if(assetType == AssetType.STOCK){
                        cph.setClosePrice(PriceFetcher.getStockPriceLastClose(assetName));
                    } else if(assetType == AssetType.CRYPTOCURRENCY){
                        cph.setClosePrice(PriceFetcher.getCryptoPriceLastClose(assetName));
                    } else if(assetType == AssetType.COMMODITY){
                        cph.setClosePrice(PriceFetcher.getCommodityPriceLastClose(assetName));
                    }
                    cph.setClosePriceTimestamp(java.time.LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());
                    closePriceHistoryRepository.save(cph);
                }
            }

        }


    }
}
