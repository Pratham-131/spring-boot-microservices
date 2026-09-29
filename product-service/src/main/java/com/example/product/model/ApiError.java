package com.example.product.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record ApiError(@JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
					   int status, String error, String message) {}