var path = require('path');

module.exports = {
    entry: {
        indexEntry: "./index.tsx"
    },
    output: {
        path: path.resolve(__dirname, 'bundle/'),
        filename: '[name]-bundle.js'
    },
    resolve: {
        modules: [
            path.resolve(__dirname, 'node_modules'),
            path.resolve(__dirname, 'src/main/ts')
        ],
        extensions: ['.ts', '.tsx', '.js', '.json']
    },
    devtool: "source-map",
    module: {
        rules: [{
            test: /\.(tsx?)|(js)$/,
            exclude: /node_modules/,
            loader: "babel-loader"
        }]
    },
    optimization: {
        minimize: false
    },
    devServer: {
        publicPath: '/bundle',
        compress: false,
        port: 9900
    }
};