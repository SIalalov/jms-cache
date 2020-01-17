package ru.test.ejb.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Активировать JAX-RS и задать базовый путь к REST
 */
@ApplicationPath("/rest")
public class JaxRsActivator extends Application {
}
