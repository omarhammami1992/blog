package com.blog.soat.repository;

import java.util.List;
import com.blog.soat.entity.Seat;

public interface TrainRepository {
   List<Seat> find(String trainId);
}
