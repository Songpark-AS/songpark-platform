
const through2 = require('through2');
const gulp = require('gulp');
const sass = require('gulp-dart-sass');
//const sass = require('sass');
const plumber = require("gulp-plumber");
const sourcemaps = require('gulp-sourcemaps');
const autoprefixer = require("gulp-autoprefixer");
const csso = require("gulp-csso");
var Fiber = require("fibers");
const log = require('fancy-log'); // instead of gulp-utils

//sass.compiler = require('sass');  // using dart sass for latest features

const paths = {
  backoffice: {
    src: './src/backoffice/app.scss',
    dest: "../../frontend/backoffice/public/css",
  },
  teacher: {
    src: './src/teacher/app.scss',
    dest: "../../frontend/teacher/www/css",
  },
  student: {
    src: './src/student/app.scss',
    dest: "../../frontend/student/www/css",
  }
};

function build_sass_backoffice() {
  return gulp
    .src(paths.backoffice.src)
    .pipe(plumber({
      errorHandler: function (err) {
        console.log(err.message);
        this.emit('end');
      }
    }))
    .pipe(sourcemaps.init())
    .pipe(sass({
      fiber: Fiber,
      outputStyle: 'compressed'
    }))
    .pipe(autoprefixer({
      cascade: false
    }))
    .pipe(csso({
      restructure: true,
      sourceMap: true,
      debug: false
    }))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(paths.backoffice.dest));
}

function build_sass_teacher() {
  return gulp
    .src(paths.teacher.src)
    .pipe(plumber({
      errorHandler: function (err) {
        console.log(err.message);
        this.emit('end');
      }
    }))
    .pipe(sourcemaps.init())
    .pipe(sass({
      fiber: Fiber,
      outputStyle: 'compressed'
    }))
    .pipe(autoprefixer({
      cascade: false
    }))
    .pipe(csso({
      restructure: true,
      sourceMap: true,
      debug: false
    }))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(paths.teacher.dest));
}


function build_sass_student() {
  return gulp
    .src(paths.student.src)
    .pipe(plumber({
      errorHandler: function (err) {
        console.log(err.message);
        this.emit('end');
      }
    }))
    .pipe(sourcemaps.init())
    .pipe(sass({
      fiber: Fiber,
      outputStyle: 'compressed'
    }))
    .pipe(autoprefixer({
      cascade: false
    }))
    .pipe(csso({
      restructure: true,
      sourceMap: true,
      debug: false
    }))
    // deal with mtime on file resulting css file
    .pipe(through2.obj( function( file, enc, cb ) {
        let date = new Date();
        file.stat.atime = date;
        file.stat.mtime = date;
        cb( null, file );
    }))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(paths.student.dest));
}

function watch_backoffice() {
  gulp.watch('./src/backoffice/**/*.scss', build_sass_backoffice);
}

function watch_teacher() {
  gulp.watch('./src/teacher/**/*.scss', build_sass_teacher);
}

function watch_student() {
  gulp.watch('./src/student/**/*.scss', build_sass_student);
}

function watch_sass() {
  gulp.watch('./src/**/*.scss', build_sass);
}

exports.default = build_sass_teacher;
exports.watch = watch_sass;
exports.backoffice_app = watch_backoffice;
exports.teacher_app = watch_teacher;
exports.student_app = watch_student;

exports.build_backoffice_app = build_sass_backoffice;
exports.build_teacher_app = build_sass_teacher;
exports.build_student_app = build_sass_student;
