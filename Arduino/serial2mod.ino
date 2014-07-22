//Autor: Dailos Reyes Diaz
//Este sketch esta diseñado para ser cargado en un robot 3pi de la marca POLOLU.
//Se usa en conjunto con una aplicación para android que controla remotamente
//el robot.
#include <TimedAction.h>
#include <serial.h>
#include <Servo.h>
#include <Pololu3pi.h>
#include <PololuQTRSensors.h>
#include <OrangutanMotors.h>
#include <OrangutanAnalog.h>
#include <OrangutanLEDs.h>
#include <OrangutanLCD.h>
#include <OrangutanPushbuttons.h>
#include <OrangutanBuzzer.h>
#include <PololuWheelEncoders.h>

int entrada = 0;
int lenght = 0;
int estado = 0;
int contador = 0;
int pack[100]; //pack[0] contiene el opcode, el resto son argumentos
double distancias[171]; //array que almacena las distancias medidas por el sharp de 150
int checksum1 = 0; //el checksum tiene dos partes
int checksum2 = 0;
int velocidadactual = 0; //es el control de velocidad para los comandos de acelerar y decelerar fijos

boolean ascendente = true;

//3pi vars

#define enc1A 19
#define enc1B 12
#define enc2A 13
#define enc2B 7
#define proxW 8
#define proxN 2
#define proxE 4
int obstaculoN = 0;
int obstaculoW = 0;
int obstaculoE = 0;
int sharp150 = A7;
int sharpvalue = 0;
int servo = 9;
int vel = 0;
int counter = 0;
long randnum = 0;
int servogrados = 0;
PololuWheelEncoders encoders;
Servo servom;

TimedAction timedAction = TimedAction(10, sistemaMap);
 
void setup() { 
  Serial.begin(9600);
  encoders.init(enc1A, enc1B, enc2A, enc2B);
  pinMode(proxW, INPUT);
  pinMode(proxN, INPUT);
  pinMode(proxE, INPUT);
  servom.attach(servo);
  servom.write(0);
}

//Método para calcular el checksum analizando la 
//estructura del paquete.
boolean checkcsum() {
  short chksum = calcChecksum();
//  Serial.print("Checksum calculado : ");
//  Serial.println(chksum);
  if (( chksum == checksum1 << 8) | checksum2)
    return true;
  return false;
}
  
//Nos movemos por el paquete almacenando en c 
//el resultado del checksum calculado.
int calcChecksum() {
  unsigned int c = 0;
  int n = lenght - 2;
  int posicion = 0;
  
  while (n > 1) {
    c = c + ((pack[posicion]<<8) | (pack[(posicion + 1)]));
    n = n - 2;
    posicion = posicion + 2;
  }
  if (n > 0)
    c = c ^ pack[(posicion + 1)];
    
  return(c);
}
    
    
//Este método es llamado cuando cuando se alcanza
//el último estado en el autómata y se procede a 
//ejecutar un comando, comprobando antes el checksum.
void muestracepta() {
//  Serial.println("Comando aceptado:");  
//  Serial.flush();  
//  Serial.print("0x");   
//  Serial.println(pack[0], HEX);
//  Serial.flush();
//  Serial.print("el tamano es ");
//  Serial.println(lenght);  
//  Serial.print("el checksum1 = ");
//  Serial.println(checksum1); 
//  Serial.print("el checksum2 = ");
//  Serial.println(checksum2); 
  if (checkcsum()) {
//    Serial.println("los Checksums coinciden");
    ejecutar();
  } else {
//    Serial.println("los checksums no coinciden");
  }
  estado = 0;
}

//el acelerar libre tan solo modifica el valor de la 
//velocidad para los motores.
void acelera(int velocidad) {
  vel = velocidad;
}

//aumenta la velocidad en 30
void acelerafijo() {
  if (velocidadactual < 210)
     velocidadactual = velocidadactual + 30;
  acelera(velocidadactual);
}

//disminuye la velocidad en 30
void desacelerafijo() {
  if (velocidadactual > -210)
      velocidadactual = velocidadactual - 30;
  acelera(velocidadactual);
}

//pone los motores a 0
void parada() {
  OrangutanMotors::setSpeeds(0, 0);
  vel = 0;
  velocidadactual = 0;
}
  
//recibe como parametro un numero del 0 al 3
//para ejecutar uno de los cuatro giros disponibles.
void girar(int direccion) {
  if (direccion == 0) { //derecha
    OrangutanMotors::setSpeeds(130, -130);
//    Serial.println("Girando a la derecha");
  } else if (direccion == 1) { //izquierda
    OrangutanMotors::setSpeeds(-130, 130);
//    Serial.println("Girando a la izquierda");
    } else if (direccion == 2) { //derecha
    OrangutanMotors::setSpeeds(70, -70);
//    Serial.println("Girando a la izquierda");
    } else if (direccion == 3) { //izquierda
    OrangutanMotors::setSpeeds(-70, 70);
//    Serial.println("Girando a la derecha");
  }
  delay(150);
  OrangutanMotors::setSpeeds(vel, vel);
}

//selecciona el comando que se desea invocar.
void ejecutar() {
  int op = pack[0];
  switch (op) {
    case 1: //0x1 = acelerar libre
      acelera(pack[2]);
      break;
    case 2:
      girar(pack[2]); //0x2 = giro de 90 o 45 grados segun argumento
      break;
    case 3:
      acelerafijo(); //0x3 = acelera una cantidad fija
      break;
    case 4:
      desacelerafijo(); //0x4 = desacelera una cantidad fija
      break;
    case 5:
      parada(); //0x5 = frena el coche en seco
      break;
    default:
//      Serial.println("Comando no implementado");
      break;
  }
}  

//Método encargado de mover el servo por los 160 grados 
//de rango, a la vez que almacena las medidas en el array.
//Devuelve cinco valores de los 160 posibles.
void sistemaMap() {
  distancias[servogrados] = mide();
  switch (servogrados) {
    case 0: 
      if (distancias[servogrados] < 60) {
        Serial.write(0);
      } else {
        Serial.write(1);
      }
      break;
    case 35: 
      uint8_t num[2];
      if (distancias[servogrados] < 60) {
        num[0] = 0;
        Serial.write(num, 2);
      } else {
        num[0] = 1;
        Serial.write(num, 2);
      }
      break;
    case 80: 
      uint8_t num2[3];
      if (distancias[servogrados] < 60) {
        num2[0] = 0;
        Serial.write(num2, 3);
      } else {
        num2[0] = 1;
        Serial.write(num2, 3);
      }
      break;
    case 125: 
      uint8_t num3[4];
      if (distancias[servogrados] < 60) {
        num3[0] = 0;
        Serial.write(num3, 4);
      } else {
        num3[0] = 1;
        Serial.write(num3, 4);
      }
      break;
    case 160: 
      uint8_t num4[5];
      if (distancias[servogrados] < 60) {
        num4[0] = 0;
        Serial.write(num4, 5);
      } else {
        num4[0] = 1;
        Serial.write(num4, 5);
      }
      break;
  }
  if (ascendente) {
    if (servogrados == 160) {
      ascendente = false;
      servogrados--;
    } else {
      servogrados++;
    }
  } else {
    if (servogrados == 0) {
      ascendente = true;
      servogrados++;
    } else {
      servogrados--;
    }
  }
  servom.write(servogrados); 
}

//Método encargado de traducir las lecturas analógicas
//del sensor sharp en valores de medida en centímetros.
double mide() {
  double acotada;
  double bruto;
  double resultado;
  //150cm es el maximo, 14cm es el minimo
  //la funcion no es lineal, asi que dividimos por tramos
  sharpvalue = analogRead(sharp150);
  if (sharpvalue < 70)
    sharpvalue = 70; 
  if (sharpvalue > 314) { //tramo superior 14 - 40 cm 545 - 315
    acotada = sharpvalue - 315;
    bruto = ((26 * acotada) / 230);
    resultado = bruto - 26;
    if (resultado < 0)
      resultado = (resultado * -1); 
    resultado = resultado + 14;
  } else if (sharpvalue > 255) { //tramo 2º 40 - 50 cm 315 - 256
    acotada = sharpvalue - 256;
    bruto = ((10 * acotada) / 59);
    resultado = bruto - 10;
    if (resultado < 0)
      resultado = (resultado * -1); 
    resultado = resultado + 40;
  } else if (sharpvalue > 184) { //tramo 3º 50 - 70 cm 256 - 185
    acotada = sharpvalue - 185;
    bruto = ((20 * acotada) / 71);
    resultado = bruto - 20;
    if (resultado < 0)
      resultado = (resultado * -1); 
    resultado = resultado + 50;  
   } else if (sharpvalue > 136) { //tramo 4º 70 - 100 cm 185 - 137
    acotada = sharpvalue - 137;
    bruto = ((30 * acotada) / 48);
    resultado = bruto - 30;
    if (resultado < 0)
      resultado = (resultado * -1); 
    resultado = resultado + 70;  
   } else { //tramo inferior 100 - 150 cm 137 - 70
    acotada = sharpvalue - 70;
    bruto = ((50 * acotada) / 67);
    resultado = bruto - 50;
    if (resultado < 0)
      resultado = (resultado * -1); 
    resultado = resultado + 100;     
   }
  return resultado;
}

//en el loop tenemos el autómata encargado
//del control de ejecución.
void loop() { 
  timedAction.check();  
  if(Serial.available() >= 5) {
    for (int i = 0; i < Serial.available(); i++) {
       entrada = Serial.read() * 256;
       entrada = entrada + Serial.read();
       switch (estado) {
         case 0:
//            Serial.println("case 0 necesita 0xFA"); 
//            Serial.println("proporcionado");
//            Serial.println(entrada);
//            Serial.println("fin");
            if (entrada == 0xFA)
              estado = 1;
            break;
         case 1:
//            Serial.println("case 1 necesita 0xFB"); 
            if (entrada == 0xFB) {
              estado = 2;
            } else if (entrada == 0xFA) {
              estado = 1;
            }
            break;
         case 2:
//            Serial.println("case 2 lenght");
            lenght = entrada;
            estado = 3;
            break;
         case 3: 
//            Serial.println("case 3 pack");
            if (contador < (lenght - 3)) { //lenght - 2 es el tamaño del pack, -3 por empezar en 0
              pack[contador] = entrada;
              contador++;
              estado = 3;
            } else {
              pack[contador] = entrada;
              contador = 0;
              estado = 4;
            }
            break;
         case 4:
//            Serial.println("case 4 checksums");
            if (contador == 0) {
              checksum1 = entrada;
              contador++;
              estado = 4;
            } else {
              checksum2 = entrada;
              contador = 0;
              muestracepta();
            }
            break;
       }
    }    
  } else if(Serial.available() >= 2) {
//    Serial.println(Serial.available());
    entrada = Serial.read() * 256;
    entrada = entrada + Serial.read();
    switch (estado) {
      case 0:
//        Serial.println("case 0 necesita 0xFA"); 
//        Serial.println("proporcionado");
//        Serial.println(entrada);
//        Serial.println("fin");
        if (entrada == 0xFA)
          estado = 1;
        break;
      case 1:
//        Serial.println("case 1 necesita 0xFB"); 
        if (entrada == 0xFB) {
          estado = 2;
        } else if (entrada == 0xFA) {
          estado = 1;
        }
        break;
      case 2:
//        Serial.println("case 2 lenght");
        lenght = entrada;
        estado = 3;
        break;
      case 3: 
//        Serial.println("case 3 pack");
        if (contador < (lenght - 3)) { //lenght - 2 es el tamaño del pack, -3 por empezar en 0
          pack[contador] = entrada;
          contador++;
          estado = 3;
        } else {
          pack[contador] = entrada;
          contador = 0;
          estado = 4;
        }
        break;
      case 4:
//        Serial.println("case 4 checksums");
        if (contador == 0) {
          checksum1 = entrada;
          contador++;
          estado = 4;
        } else {
          checksum2 = entrada;
          contador = 0;
          muestracepta();
        }
        break;
    }
  } else if (Serial.available() == 1) { //ejecución de comando en modo directo.
     entrada = Serial.read();
     if (entrada == 100) {
       acelerafijo();
     } else if (entrada == 111) {
       desacelerafijo();
     } else if (entrada == 122) {
       girar(1); //izq
     } else if (entrada == 127) {
       girar(0); //der
     } else if (entrada == 85) {
       girar(3); //izq
     } else if (entrada == 60) {
       girar(2); //der
     } else if (entrada == 70) {
       parada(); //der
     }
  }  
  //parte de la accion de los sensores de proximidad.
  randnum = random(2);
  obstaculoN = digitalRead(proxN);
  obstaculoW = digitalRead(proxW);
  obstaculoE = digitalRead(proxE);
  if ((obstaculoW == LOW)&&(obstaculoE == LOW)) {
    OrangutanMotors::setSpeeds(0, 0);
    delay(50);
    OrangutanMotors::setSpeeds(-100, -100);
   delay(300);
  } else if (obstaculoN == LOW) {
    OrangutanMotors::setSpeeds(-150, -150);
    delay(300);
    if (randnum == 0) {
      OrangutanMotors::setSpeeds(-130, 130);
    } else {
      OrangutanMotors::setSpeeds(130, -130);
    }
    delay(150);
  } else if (obstaculoW == LOW) {
      OrangutanMotors::setSpeeds(80, 40);
      delay(120);
  } else if (obstaculoE == LOW) {
      OrangutanMotors::setSpeeds(40, 80);
      delay(120);
  } else {
    OrangutanMotors::setSpeeds(vel, vel); //esta es la orden que mueve el 3pi
  }
   
} 
