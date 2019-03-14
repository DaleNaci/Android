package com.example.raceproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {

    // OBJECT DECLARATION
    Thread          gameThread;
    SurfaceHolder   holder;
    Bitmap          mario;
    Bitmap          bowser;
    Bitmap          marioHit;
    Paint           timerProperty;
    Paint           pointsProperty;
    Display         screenDisplay;
    Point           sizeOfScreen;
    String          sensorOutput;
    RelativeLayout  layout;
    MediaPlayer     backgroundMusic;
    MediaPlayer     hitMusic;
    MediaPlayer     finishMusic;
    GameSurface     gameSurface;


    // BASIC DECLARATION
    int                 screenWidth;
    int                 screenHeight;
    volatile boolean    running;
    int                 marioX;
    int                 marioY;
    int                 bowserX;
    int                 bowserY;
    int                 bowserVelocity;
    int                 x;
    int                 marioVelocity;
    int                 hitTimer;
    int                 points;
    int                 timer;
    int                 tapMultiplier;
    boolean             dodged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameSurface = new GameSurface(this);
        setContentView(gameSurface);
    }

    @Override
    protected void onPause(){
        super.onPause();
        gameSurface.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        gameSurface.resume();
    }



    //----------------------------GameSurface Below This Line--------------------------
    public class GameSurface extends SurfaceView implements Runnable, SensorEventListener {


        public GameSurface(Context context) {
            super(context);

            // OBJECT DEFINING
            holder          =   getHolder();
            mario           =   BitmapFactory.decodeResource( getResources(), R.drawable.    mario );
            bowser          =   BitmapFactory.decodeResource( getResources(), R.drawable.   bowser );
            marioHit        =   BitmapFactory.decodeResource( getResources(), R.drawable.mario_hit );
            screenDisplay   =   getWindowManager().getDefaultDisplay();
            timerProperty   =   new Paint();
            pointsProperty  =   new Paint();
            sizeOfScreen    =   new Point();
            layout          =   findViewById(R.id.layout);
            backgroundMusic =   MediaPlayer.create(MainActivity.this, R.raw. theme_song);
            hitMusic        =   MediaPlayer.create(MainActivity.this, R.raw.   hit_song);
            finishMusic     =   MediaPlayer.create(MainActivity.this, R.raw.finish_song);


            // BASIC DEFINING
            screenWidth     =   sizeOfScreen.x;
            screenHeight    =   sizeOfScreen.y;
            running         =   false;
            marioX          =   400;
            marioY          =   1000;
            bowserX         =   (int)(Math.random() * 800);
            bowserY         =   -bowser.getHeight();
            bowserVelocity  =   15;
            x               =   200;
            marioVelocity   =   0;
            sensorOutput    =   "";
            hitTimer        =   0;
            timer           =   1860;
            points          =   0;
            dodged          =   true;
            tapMultiplier   =   1;

            screenDisplay.getSize(sizeOfScreen);


            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometerSensor,sensorManager.SENSOR_DELAY_NORMAL);

            timerProperty. setTextSize( 260 );
            pointsProperty.setTextSize( 120 );
        }

        @Override
        public void run() {
            while (running){
                if (!holder.getSurface().isValid())
                    continue;

                Canvas canvas = holder.lockCanvas();
                canvas.drawRGB(67,176,71);
                canvas.drawText(sensorOutput, x, 200, timerProperty);

                if (bowserY > 1500) {
                    bowserY = -bowser.getHeight();
                    bowserX = (int)(Math.random() * 800);
                    if (dodged)
                        points++;
                    dodged = true;
                }

                marioX  +=  tapMultiplier *  marioVelocity;
                bowserY +=  tapMultiplier * bowserVelocity;

                if (marioX < 0)
                    marioX = 0;
                if (marioX > 800)
                    marioX = 800;

                if (marioX > (bowserX - mario.getWidth()) && marioX < (bowserX + bowser.getWidth()) && marioY > (bowserY - mario.getHeight()) && (marioY < bowserY + bowser.getHeight())) {
                    canvas.drawBitmap( marioHit,  marioX,  marioY, null);
                    hitTimer = 30;
                    if (dodged) {
                        points--;
                        if (hitMusic.isPlaying()) {
                            hitMusic.stop();
                            try {
                                hitMusic.prepare();
                            } catch (Exception e) { e.printStackTrace(); }
                            hitMusic.start();
                        } else {
                            hitMusic.start();
                        }
                    }
                    dodged = false;
                } else {
                    if (hitTimer > 0)
                        canvas.drawBitmap( marioHit, marioX, marioY, null);
                    else
                        canvas.drawBitmap( mario,  marioX,  marioY, null);
                }

                if (hitTimer > 0)
                    hitTimer--;

                canvas.drawBitmap(bowser, bowserX, bowserY, null);

                canvas.drawText(Integer.toString(points), 50, 100, pointsProperty);
                if (timer / 60 < 10)
                    canvas.drawText(Integer.toString(timer / 60), 440, 220, timerProperty);
                else
                    canvas.drawText(Integer.toString(timer / 60), 400, 220, timerProperty);

                holder.unlockCanvasAndPost(canvas);

                if (timer <= 0) {
                    running = false;
                    if (backgroundMusic.isPlaying()) {
                        backgroundMusic.stop();
                        if (!finishMusic.isPlaying()) {
                            finishMusic.start();
                        }
                    }
                }
                timer--;

                if (!backgroundMusic.isPlaying()) {
                    backgroundMusic.start();
                }


            }
        }

        public void resume(){
            running=true;
            gameThread=new Thread(this);
            gameThread.start();
        }

        public void pause() {
            running = false;
            while (true) {
                try {
                    gameThread.join();
                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float xTilt     =   event.values[0];
            marioVelocity   =   -(int)xTilt * 3;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    public void tapped(View v) {
        if (tapMultiplier == 1)
            tapMultiplier = 2;
        if (tapMultiplier == 2)
            tapMultiplier = 1;
    }
}
