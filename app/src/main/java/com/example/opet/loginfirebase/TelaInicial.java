package com.example.opet.loginfirebase;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.iwf.photopicker.PhotoPicker;

/**
 * Created by opet on 21/08/2018.
 */

public class TelaInicial extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private ImageView imgSelected;
    private StorageReference mStorageRef;
    private ArrayList<String> photos;

    EditText editNome, editIdioma, editGraduacao, editNascimento;
    ListView listV_dados;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    private List<Pessoa> listPessoa = new ArrayList<Pessoa>();
    private ArrayAdapter<Pessoa> arrayAdapterPessoa;

    Pessoa pessoaSelecionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_inicial);

        photos = new ArrayList<>();
        imgSelected = findViewById(R.id.imgSelected);
        mStorageRef = FirebaseStorage.getInstance().getReference();

        editNome = (EditText)findViewById(R.id.editNome);
        editIdioma = (EditText)findViewById(R.id.editIdioma);
        editGraduacao = (EditText)findViewById(R.id.editGraduacao);
        editNascimento = (EditText)findViewById(R.id.editNascimento);
        listV_dados = (ListView)findViewById(R.id.listView_dados);

        inicializarFirebase();
        eventoDataBase();

        listV_dados.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                pessoaSelecionada = (Pessoa)parent.getItemAtPosition(i);
                editNome.setText(pessoaSelecionada.getNome());
                editGraduacao.setText(pessoaSelecionada.getGraduacao());
                editIdioma.setText(pessoaSelecionada.getIdioma());
                editNascimento.setText((CharSequence) pessoaSelecionada.getNascimento());
            }
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String emailCurrent = currentUser.getEmail();

        TextView emailUser = findViewById(R.id.email);
        emailUser.setText("Bem vindo, " + emailCurrent);

    }

    public void photoPickerFunction(View view){
        PhotoPicker.builder()
                .setPhotoCount(1)
                .setShowCamera(true)
                .setShowGif(true)
                .setPreviewEnabled(false)
                .start(this, PhotoPicker.REQUEST_CODE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
                imgSelected.setImageURI(Uri.parse(photos.get(0)));
            }
        }
    }

    private void resetForm(){
        photos.clear();
        imgSelected.setImageResource(0);
    }

    public void sendPhotoFunction(View view) {
        if(photos.size() > 0){
            Uri file = Uri.fromFile(new File(photos.get(0)));
            StorageReference photoRef = mStorageRef.child("images");
            photoRef.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(TelaInicial.this, "Arquivo Enviado com sucesso!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(TelaInicial.this, "Falha ao enviar arquivo.", Toast.LENGTH_SHORT).show();
                }
            });
            resetForm();
        }else{
            Toast.makeText(this, "Nenhum arquivo carregado.", Toast.LENGTH_SHORT).show();
        }
    }

    private void inicializarFirebase() {
        FirebaseApp.initializeApp(TelaInicial.this);
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
        databaseReference = firebaseDatabase.getReference();
    }

    private void eventoDataBase()
    {
        databaseReference.child("Pessoa").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listPessoa.clear();
                for (DataSnapshot objetoSnapshot:dataSnapshot.getChildren()){
                    Pessoa p = objetoSnapshot.getValue(Pessoa.class);
                    listPessoa.add(p);
                }
                arrayAdapterPessoa = new ArrayAdapter<Pessoa>(TelaInicial.this, android.R.layout.simple_list_item_1, listPessoa);
                listV_dados.setAdapter(arrayAdapterPessoa);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.menu_novo){
            Pessoa p = new Pessoa();
            p.setId(UUID.randomUUID().toString());
            p.setNome(editNome .getText().toString());
            p.setIdioma(editIdioma.getText().toString());
            p.setGraduacao(editGraduacao.getText().toString());
            // p.setNascimento(editNascimento.getText().toString());
            databaseReference.child("Pessoa").child(String.valueOf(p.getId())).setValue(p);
            limparCampos();
        }
        else
            if (id == R.id.menu_atualizar)
            {
                Pessoa p = new Pessoa();
                p.setId(pessoaSelecionada.getId());
                p.setNome(editNome.getText().toString().trim());
                //p.setNascimento(editNascimento.getText().toString().trim());
                p.setGraduacao(editGraduacao.getText().toString().trim());
                p.setIdioma(editIdioma.getText().toString().trim());
                databaseReference.child("Pessoa").child(String.valueOf(p.getId())).setValue(p);
                limparCampos();
            }
            else
                if (id == R.id.menu_deletar)
                {
                    Pessoa p = new Pessoa();
                    p.setId(pessoaSelecionada.getId());
                    databaseReference.child("Pessoa").child(String.valueOf(p.getId())).removeValue();
                }
        return true;
    }

    private void limparCampos()
    {
        editNome.setText("");
        editGraduacao.setText("");
        editIdioma.setText("");
        editNascimento.setText("");
    }

    public void Sair(View view){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}

