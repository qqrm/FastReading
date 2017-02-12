package com.pashkobohdan.fastreading;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.daimajia.swipe.util.Attributes;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.pashkobohdan.fastreading.library.bookTextWorker.BookInfo;
import com.pashkobohdan.fastreading.library.bookTextWorker.BookInfoFactory;
import com.pashkobohdan.fastreading.library.bookTextWorker.BookInfosList;
import com.pashkobohdan.fastreading.library.fileSystem.file.InternalStorageFileHelper;
import com.pashkobohdan.fastreading.library.fileSystem.newFileOpeningThread.FileOpenThread;
import com.pashkobohdan.fastreading.library.ui.dialogs.BookEditDialog;
import com.pashkobohdan.fastreading.library.ui.lists.booksList.BooksRecyclerViewAdapter;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import ir.sohreco.androidfilechooser.ExternalStorageNotAvailableException;
import ir.sohreco.androidfilechooser.FileChooserDialog;

import static com.pashkobohdan.fastreading.library.fileSystem.file.InternalStorageFileHelper.INTERNAL_FILE_EXTENSION;

public class MyBooks extends AppCompatActivity implements FileChooserDialog.ChooserListener {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private RecyclerView booksRecyclerView;
    private RecyclerView.Adapter booksAdapter;

    private FloatingActionsMenu booksFloatingActionsMenu;
    private FloatingActionButton floatingActionButtonOpenFile,
            floatingActionButtonDownloadBook,
            floatingActionButtonCreateBook;


    enum BooksSortTypes {
        BY_LAST_OPENING,
        BY_PROGRESS,
        BY_NAME,
        BY_BACK_NAME,
        BY_AUTHOR,
        BY_BACK_AUTHOR
    }

    BooksSortTypes booksSortType = BooksSortTypes.BY_LAST_OPENING;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_books);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My books");
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        booksFloatingActionsMenu = (FloatingActionsMenu) findViewById(R.id.book_list_fab_menu);
        floatingActionButtonOpenFile = (FloatingActionButton) findViewById(R.id.book_list_open_pdf_fb2_txt_file);
        floatingActionButtonDownloadBook = (FloatingActionButton) findViewById(R.id.book_list_download_book_from_cloud);
        floatingActionButtonCreateBook = (FloatingActionButton) findViewById(R.id.book_list_create_new_book);


        booksRecyclerView = (RecyclerView) findViewById(R.id.books_recycler_view);
        booksRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        initFABsListeners();

        // if data is already loaded
        if (BookInfosList.getAll().size() == 0) {
            initBookInfoDatas();
        }

        if (booksAdapter == null) {
            initBooksListAdapter();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshBookList();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onSelect(String path) {
        selectOpeningFile(new File(path));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooserDialog();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_books, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_sort_by_last_open:
                if (!item.isChecked()) {
                    booksSortType = BooksSortTypes.BY_LAST_OPENING;
                    refreshBookList();
                    item.setChecked(true);
                }

                return true;

            case R.id.action_sort_by_progress:
                if (!item.isChecked()) {
                    booksSortType = BooksSortTypes.BY_PROGRESS;
                    refreshBookList();
                    item.setChecked(true);
                }

                return true;


            case R.id.action_sort_by_name:
                if (!item.isChecked()) {
                    booksSortType = BooksSortTypes.BY_NAME;
                    refreshBookList();
                    item.setChecked(true);
                }

                return true;

            case R.id.action_back_sort_by_name:
                if (!item.isChecked()) {
                    booksSortType = BooksSortTypes.BY_BACK_NAME;
                    refreshBookList();
                    item.setChecked(true);
                }

                return true;

            case R.id.action_sort_by_author:
                if (!item.isChecked()) {
                    booksSortType = BooksSortTypes.BY_AUTHOR;
                    refreshBookList();
                    item.setChecked(true);
                }

                return true;

            case R.id.action_back_sort_by_author:
                if (!item.isChecked()) {
                    booksSortType = BooksSortTypes.BY_BACK_AUTHOR;
                    refreshBookList();
                    item.setChecked(true);
                }

                return true;
        }


        return super.onOptionsItemSelected(item);
    }


    // business logic functions

    private void refreshBookList() {
        switch (booksSortType) {
            case BY_LAST_OPENING:
                Collections.sort(BookInfosList.getAll(), (o1, o2) ->
                        Integer.valueOf(o2.getLastOpeningDate()).compareTo(o1.getLastOpeningDate())
                );
                break;

            case BY_PROGRESS:
                Collections.sort(BookInfosList.getAll(), (o2, o1) -> {
                            if (!o1.isWasRead()) {
                                if (!o2.isWasRead()) {
                                    return 0;
                                }
                                return -1;
                            } else {
                                if (!o2.isWasRead()) {
                                    return 1;
                                } else {
                                    return Integer.valueOf((int) (100.0 * o1.getCurrentWordNumber() / o1.getWords().length))
                                            .compareTo((int) (100.0 * o2.getCurrentWordNumber() / o2.getWords().length));
                                }
                            }
                        }
                );
                break;

            case BY_NAME:
                Collections.sort(BookInfosList.getAll(), (o1, o2) -> o1.getName().compareTo(o2.getName()));
                break;

            case BY_BACK_NAME:
                Collections.sort(BookInfosList.getAll(), (o1, o2) -> o2.getName().compareTo(o1.getName()));
                break;

            case BY_AUTHOR:
                Collections.sort(BookInfosList.getAll(), (o1, o2) -> o1.getAuthor().compareTo(o2.getAuthor()));
                break;

            case BY_BACK_AUTHOR:
                Collections.sort(BookInfosList.getAll(), (o1, o2) -> o2.getAuthor().compareTo(o1.getAuthor()));
                break;
        }

        booksAdapter.notifyDataSetChanged();
    }

    private void initFABsListeners() {
        floatingActionButtonOpenFile.setOnClickListener(v -> {
            tryOpenFile();
            booksFloatingActionsMenu.collapse();
        });
        floatingActionButtonDownloadBook.setOnClickListener(v -> {

            booksFloatingActionsMenu.collapse();
        });
        floatingActionButtonCreateBook.setOnClickListener(v -> {

            booksFloatingActionsMenu.collapse();
        });
    }

    private void initBookInfoDatas() {
        for (File file : getCacheDir().listFiles((directory, fileName) -> fileName.endsWith(INTERNAL_FILE_EXTENSION))) {
            BookInfosList.add(BookInfoFactory.newInstance(file, this));
        }
    }

    private void initBooksListAdapter() {


        booksAdapter = new BooksRecyclerViewAdapter(this, BookInfosList.getAll(), (bookInfo) -> {

            if (!checkBookReady(bookInfo)) {
                return;
            }

            new BookEditDialog(this, bookInfo, () ->
                    booksAdapter.notifyItemChanged(BookInfosList.getAll().indexOf(bookInfo))).show();

        }, (bookInfo) -> {
            Toast.makeText(this, "sharing : " + bookInfo.getName(), Toast.LENGTH_SHORT).show();
        }, (bookInfo) -> {
            Toast.makeText(this, "uploading : " + bookInfo.getName(), Toast.LENGTH_SHORT).show();
        }, (bookInfo, ifConfirmed) -> {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if (bookInfo.getFile().delete()) {
                            ifConfirmed.run();
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Do you want delete this record  : " + bookInfo.getName()).setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }, (bookInfo) -> {

            if (!checkBookReady(bookInfo)) {
                return;
            }

            Intent i = new Intent(this, CurrentBook.class);
            i.putExtra("serializable_book_file", bookInfo.getFile());
            startActivity(i);

        }, (bookInfo) -> {

            Toast.makeText(this, "long click : " + bookInfo.getName(), Toast.LENGTH_SHORT).show();

        });


        ((BooksRecyclerViewAdapter) booksAdapter).setMode(Attributes.Mode.Single);
        booksRecyclerView.setAdapter(booksAdapter);

    }


    private boolean checkBookReady(BookInfo bookInfo) {
        if (bookInfo.isWasRead()) {
            if (bookInfo.getWords().length < 1) {
                new AlertDialog.Builder(this)
                        .setPositiveButton("Ok", (dialog, which) -> {
                        })
                        .setTitle("Information")
                        .setMessage("This book is empty. Try delete book and open again")
                        .create()
                        .show();

                return false;
            }
            return true;
        } else {
            new AlertDialog.Builder(this)
                    .setPositiveButton("Ok", (dialog, which) -> {
                    })
                    .setTitle("Information")
                    .setMessage("The book has not read yet.\nPlease, wait few second")
                    .create()
                    .show();

            return false;
        }
    }

    private void tryOpenFile() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            openFileChooserDialog();
        }
    }

    private void openFileChooserDialog() {
        FileChooserDialog.Builder builder =
                new FileChooserDialog.Builder(FileChooserDialog.ChooserType.FILE_CHOOSER, this)
                        .setTitle("Select a file:")
                        .setFileFormats(new String[]{".txt", ".pdf", ".fb2"});
        //.setSelectDirectoryButtonText("ENTEKHAB")
        //.setSelectDirectoryButtonBackground(R.drawable.dr)
        //.setSelectDirectoryButtonTextColor(R.color.cl)
        //.setSelectDirectoryButtonTextSize(25)
        //.setFileIcon(R.drawable.ic_file)
        //.setDirectoryIcon(R.drawable.ic_directory)
        //.setPreviousDirectoryButtonIcon(R.drawable.ic_prev_dir);
        try {
            builder.build().show(getSupportFragmentManager(), null);
        } catch (ExternalStorageNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void selectOpeningFile(File inputFile) {
        String bookName = InternalStorageFileHelper.fileNameWithoutExtension(inputFile);

        if (InternalStorageFileHelper.isFileWasOpened(this, inputFile)) {

            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        openFileWithUI(inputFile);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Do you want to rewrite this book \"" +
                    bookName +
                    "\" ?\nAll reading progress will be removed)")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener)
                    .show();

        } else {
            openFileWithUI(inputFile);
        }
    }

    private void openFileWithUI(File inputFile) {
        final Activity activity = this;
        final ProgressDialog pd = new ProgressDialog(activity);
        pd.setTitle("Reading");
        pd.setMessage("Please, wait while book loading.\nYou can use another apps at this time");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMax(100);
        pd.setProgress(0);
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();

        new FileOpenThread(inputFile, this, (o, n) -> {
            pd.setIndeterminate(false);
            pd.setProgress(n);
        }, () -> {
            pd.dismiss();

            pd.setTitle("Writing");
            pd.setMessage("Please, wait");
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.setMax(100);
            pd.setProgress(0);
            pd.setIndeterminate(true);
            pd.setCancelable(false);
            pd.show();
        }, (o, n) -> {
            pd.setIndeterminate(false);
            pd.setProgress(n);
        }, pd::dismiss, file -> {
            if (file != null) {
                addNewBookToBooksList(file);
            }
        }).start();

    }

    private void addNewBookToBooksList(File outputFile) {
        BookInfosList.add(BookInfoFactory.newInstance(outputFile, this));

        booksAdapter.notifyItemInserted(BookInfosList.getAll().size() - 1);
    }
}
