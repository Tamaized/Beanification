package tamaized.beanification.processors;

import tamaized.beanification.BeanDefinition;

import java.util.HashMap;
import java.util.Map;

public class BeanAnnotationProcessorMetadata {

	public Map<BeanDefinition<?>, BeanAnnotationProcessorClassMetadata> beans = new HashMap<>();

}
