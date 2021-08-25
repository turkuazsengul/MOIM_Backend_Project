package com.company;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;

import com.company.Main.*;

import java.lang.reflect.*;

import org.reflections.*;
import org.reflections.scanners.MethodAnnotationsScanner;

public class ApplicationContext {

    private Set<Class<?>> beanList;

    private ArrayList<String> circularClassList;

    public ArrayList<String> getCircularClassList() {
        return circularClassList;
    }

    public void setCircularClassList(ArrayList<String> circularClassList) {
        this.circularClassList = circularClassList;
    }

    public Set<Class<?>> getBeanList() {
        return beanList;
    }

    public void setBeanList(Set<Class<?>> beanList) {
        this.beanList = beanList;
    }


    //TODO: AplicationContext oluşturulduğu an lifeCycle ın kontrolü constructor method tarafından sağlanıyor.
    ApplicationContext(Class<?> mainClass) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, InvocationTargetException {
        //TODO: ilk olarak sistemde yer alan tüm "Bean" notasyonları listeye alınıyor.
        beanList();

        //TODO: ikinci aşamada "Inject" edilen class ların circular durumu kontrol ediliyor. Kontrol sonucu sistem durduruluyor yada döngüye devam ediyor.
        if (!getBeanList().isEmpty()) {
            if (checkCircularDependency()) {
                CustomInjector.startApplication(mainClass);
                crateInitMethod();
            } else {
                System.out.println("Application Context Failed: Circular Dependency");  //TODO: circular sırlaması inject edilen service e göre değişiyor.
                for (int i = 0; i < getCircularClassList().size(); i++) {
                    if (i == 0) {
                        System.out.print(getCircularClassList().get(i) + " ==> ");
                    } else if (i == getCircularClassList().size() - 1) {
                        System.out.print(getCircularClassList().get(i));
                    } else {
                        System.out.print(getCircularClassList().get(i) + " ==> ");
                    }
                }
                System.exit(0);
            }
        } else {
            System.out.println("Bean Listesi Boş");
        }

    }

    //TODO: class ların inject edilen classlarda döngüsel durumu boolean bir liste içinde return edilir. circular dependency durumu olan liste yalnızca true değerleri içerir.
    public Boolean checkCircularDependency() {
        Set<Class<?>> beanClassList = getBeanList();
        TreeMap<String, String> circularMapField = new TreeMap<>();
        Set<String> set = circularMapField.keySet();
        ArrayList<String> fieldRefClassList = new ArrayList<>();
        ArrayList<String> circularClassListCurrent = null;
        Boolean point = null;


        //TODO: tüm app de yer alan Bean notasyonlarının içinde yer alan field lar --(inject)-- üst class ve referans class değerleri ile bir map e setlenir.
        if (!getBeanList().isEmpty()) {
            for (Class beanClass : beanClassList) {
                for (Field a : beanClass.getDeclaredFields()) {
                    String clazz = a.getDeclaringClass().getSimpleName();
                    String subClass = a.getType().getSimpleName();
                    fieldRefClassList.add(subClass);
                    circularMapField.put(clazz, subClass);
                }
            }
            //TODO: setlenmiş olan mapde her bir key değerine karşılık gelen tüm map değerleri kontrol edilerek, eşleşme durumu boolean olarak return edilir.
            circularClassListCurrent = new ArrayList<>(set);

            if (fieldRefClassList.size() > 1 && circularClassListCurrent.size() > 0) {
                for (int i = 1; i < fieldRefClassList.size(); i++) {
                    for (int j = 0; j < i; j++) {
                        if (fieldRefClassList.get(i).equals(circularClassListCurrent.get(j))) {
                            circularClassListCurrent.add(i, fieldRefClassList.get(i));
                            point = false;
                            break;
                        } else {
                            point = true;
                        }
                    }
                }
            } else {
                point = true;
            }
        } else {
            point = true;
        }

        //TODO: eşleşme sonuçlarını içeren liste local listeye setlenir.
        setCircularClassList(circularClassListCurrent);
        return point;
    }

    //TODO: sistem içerisinde yer alan @Inıt notasyonları objelenerek her bir method çalıştırılır.
    public void crateInitMethod() throws InstantiationException, IllegalAccessException, ClassNotFoundException, InvocationTargetException {
        ArrayList<Method> list1 = addInitBeanList();
        for (Method a : list1) {
            Class<?> a1 = Class.forName("com.company." + a.getDeclaringClass().getSimpleName());
            if (!getBeanList().isEmpty() && getBeanList().contains(a1)) {
                Object service = a1.newInstance();
                if (a.invoke(service) != null) {
                    a.invoke(service);
                }
            }else{
                System.out.println("\n" + a.getDeclaringClass().getSimpleName() + " Class'ına ait @Bean notasyonu olmadığı için -- " + a.getName() + " -- methodu çalıştırılamadı!");
            }
        }
    }

    //TODO: Main class içerisinde string olarak gönderilen ClassName in objesinin dönüşünü sağlayan method.
    //TODO: Sorun1= a.newInstance kullanımında MyDao objesini oluşturmuyor. Ek olarak implement edildiği class a gitme ihtiyacı mevcut.
    //TODO: Sorun2= MyDaoImpl methodu @Bean notasyonlu bir method olmadığında getService() methodu hata dönüyor tespit edilmeli!
    public Object getBeanByName(String className) {
        try {
            Class<?> a = Class.forName("com.company." + className);
            if (!getBeanList().isEmpty() && getBeanList().contains(a)) {
                return CustomInjector.getService(a);
//            return a.newInstance();
            }else{
                System.out.println("\n" + className + " Class'ına ait @Bean notasyonu olmadığı için nesne oluşturulamadı!");
                System.exit(0);
                return null;
            }
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    //TODO: @Inıt notasyonlarının methodları liste olarak return ediliyor.
    public ArrayList<Method> addInitBeanList() {
        Reflections reflections = new Reflections("com.company", new MethodAnnotationsScanner());
        Set<Method> methodList = reflections.getMethodsAnnotatedWith(Init.class);
        ArrayList<Method> list = new ArrayList<>(methodList);
        return list;
    }

    //TODO: @Bean notasyonlarının bulunduğu class bilgileri local değişkene liste olarak setleniyor.
    public void beanList() {
        Reflections reflections = new Reflections("com.company");
        Set<Class<?>> beans = reflections.getTypesAnnotatedWith(Bean.class);
        setBeanList(beans);
    }

    //TODO: Tüm @Bean notasyonları liste şeklinde yazdırılıyor.
    //TODO: implement class ların name: alanı implement edildiği class a göre setlenmeli. İncelenicek.
    public void displayAllBeans() {
        try {
            Set<Class<?>> beans = getBeanList();
            System.out.println("\n--Bean List--");
            for (Class<?> bean : beans) {
                System.out.println(
                        "name: " + bean.getSimpleName() + ","
                                + " Type: " + bean.getTypeName()
                );
            }
        } catch (Exception e) {
            if (beanList.isEmpty()) {
                System.out.println("Bean Listesi Boş: " + e);
            } else {
                System.out.println(e);
            }

        }
    }


}
